/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.restapi.core.service;

import static com.epam.digital.data.platform.restapi.core.utils.KafkaUtils.getKafkaHeader;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.ResponseHeaders;
import com.epam.digital.data.platform.restapi.core.exception.KafkaCephResponseNotFoundException;
import com.epam.digital.data.platform.restapi.core.exception.NoKafkaResponseException;
import com.epam.digital.data.platform.starter.kafka.config.properties.KafkaProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;

public abstract class GenericService<I, O> implements KafkaService<I, O> {

  static final String DIGITAL_SEAL_KAFKA_HEADER = "digital-seal";

  private final Logger log = LoggerFactory.getLogger(GenericService.class);

  private final ReplyingKafkaTemplate<String, Request<I>, String> replyingKafkaTemplate;
  private final KafkaProperties.RequestReplyHandler topics;

  @Value("${data-platform.kafka-request.signing.enabled}")
  private boolean isSigningEnabled;
  @Value("${datafactory-response-ceph.bucket}")
  private String datafactoryResponseBucket;

  @Autowired
  private DigitalSignatureService digitalSignatureService;
  @Autowired
  private TraceProvider traceProvider;
  @Autowired
  private CephService datafactoryResponseCephService;
  @Autowired
  private ObjectMapper objectMapper;

  protected GenericService(
      ReplyingKafkaTemplate<String, Request<I>, String> replyingKafkaTemplate,
      KafkaProperties.RequestReplyHandler topics) {
    this.replyingKafkaTemplate = replyingKafkaTemplate;
    this.topics = topics;
  }

  GenericService(
      ReplyingKafkaTemplate<String, Request<I>, String> replyingKafkaTemplate,
      KafkaProperties.RequestReplyHandler topics,
      DigitalSignatureService digitalSignatureService,
      TraceProvider traceProvider,
      CephService datafactoryResponseCephService,
      ObjectMapper objectMapper,
      boolean isSigningEnabled,
      String datafactoryResponseBucket) {
    this(replyingKafkaTemplate, topics);
    this.digitalSignatureService = digitalSignatureService;
    this.traceProvider = traceProvider;
    this.datafactoryResponseCephService = datafactoryResponseCephService;
    this.objectMapper = objectMapper;
    this.isSigningEnabled = isSigningEnabled;
    this.datafactoryResponseBucket = datafactoryResponseBucket;
  }

  protected abstract TypeReference<Response<O>> type();

  @Override
  public Response<O> request(Request<I> input) {
    var request = new ProducerRecord<>(topics.getRequest(), traceProvider.getRequestId(), input);

    if (isSigningEnabled) {
      String digitalSeal = digitalSignatureService.sign(input);
      String cephKey = digitalSignatureService.store(digitalSeal);

      var signatureHeader = new RecordHeader(DIGITAL_SEAL_KAFKA_HEADER, cephKey.getBytes(UTF_8));
      request.headers().add(signatureHeader);
    }

    var responseRecord = sendRequest(input, request);

    var cephResponseKeyHeaderValue =
        getKafkaHeader(responseRecord, ResponseHeaders.CEPH_RESPONSE_KEY);
    if (cephResponseKeyHeaderValue.isPresent()) {
      log.info("Reading large response from Ceph");
      return getResponseFromStorage(cephResponseKeyHeaderValue.get());
    } else {
      return fromString(responseRecord.value());
    }
  }

  private ConsumerRecord<String, String> sendRequest(
      Request<I> input, ProducerRecord<String, Request<I>> request) {
    var header = new RecordHeader(KafkaHeaders.REPLY_TOPIC, topics.getReply().getBytes());
    request.headers().add(header);

    log.info("Sending to Kafka, topic {}", request.topic());
    var replyFuture = replyingKafkaTemplate.sendAndReceive(request);

    try {
      var response = replyFuture.get(30L, TimeUnit.SECONDS);
      log.info(
          "Successfully got response from Kafka, topic: {}, key: {}",
          response.topic(),
          response.key());
      return response;
    } catch (Exception e) {
      throw new NoKafkaResponseException("No response for request: " + input, e);
    }
  }

  private Response<O> getResponseFromStorage(String key) {
    var responseContent =
        datafactoryResponseCephService
            .getAsString(datafactoryResponseBucket, key)
            .orElseThrow(
                () ->
                    new KafkaCephResponseNotFoundException(
                        "Kafka response does not exist in ceph bucket"));

    deleteProcessedContentFromStorage(key);
    return fromString(responseContent);
  }

  private Response<O> fromString(String content) {
    try {
      return objectMapper.readValue(content, type());
    } catch (JsonProcessingException e) {
      throw new RuntimeJsonMappingException(e.getMessage());
    }
  }

  private void deleteProcessedContentFromStorage(String key) {
    try {
      log.info("Deleting large payload from Ceph");
      datafactoryResponseCephService.delete(datafactoryResponseBucket, Collections.singleton(key));
    } catch (Exception e) {
      log.error("Exception while deleting processed message from ceph", e);
    }
  }
}
