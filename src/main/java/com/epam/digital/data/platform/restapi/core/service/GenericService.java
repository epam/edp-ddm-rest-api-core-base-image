package com.epam.digital.data.platform.restapi.core.service;

import static com.epam.digital.data.platform.restapi.core.utils.KafkaUtils.getKafkaHeader;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.ResponseHeaders;
import com.epam.digital.data.platform.restapi.core.exception.KafkaCephResponseNotFoundException;
import com.epam.digital.data.platform.restapi.core.exception.NoKafkaResponseException;
import com.epam.digital.data.platform.starter.restapi.config.properties.KafkaProperties.Handler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;

public abstract class GenericService<I, O> {

  static final String KAFKA_HEADER = "digital-seal";

  private final Logger log = LoggerFactory.getLogger(GenericService.class);

  private final ReplyingKafkaTemplate<String, I, String> replyingKafkaTemplate;
  private final Handler topics;

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
      ReplyingKafkaTemplate<String, I, String> replyingKafkaTemplate,
      Handler topics) {
    this.replyingKafkaTemplate = replyingKafkaTemplate;
    this.topics = topics;
  }

  GenericService(
      ReplyingKafkaTemplate<String, I, String> replyingKafkaTemplate,
      Handler topics,
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

  public Response<O> request(I input) {
    var request = new ProducerRecord<>(topics.getRequest(), traceProvider.getRequestId(), input);

    if (isSigningEnabled) {
      String digitalSeal = digitalSignatureService.sign(input);
      String cephKey = digitalSignatureService.store(digitalSeal);

      var signatureHeader = new RecordHeader(KAFKA_HEADER, cephKey.getBytes(UTF_8));
      request.headers().add(signatureHeader);
    }

    var responseRecord = sendRequest(input, request);

    var cephResponseKeyHeaderValue =
        getKafkaHeader(responseRecord, ResponseHeaders.CEPH_RESPONSE_KEY);
    if (cephResponseKeyHeaderValue.isPresent()) {
      return getResponseFromStorage(cephResponseKeyHeaderValue.get());
    } else {
      return fromString(responseRecord.value());
    }
  }

  private ConsumerRecord<String, String> sendRequest(
      I input, ProducerRecord<String, I> request) {
    var header = new RecordHeader(KafkaHeaders.REPLY_TOPIC, topics.getReplay().getBytes());
    request.headers().add(header);

    var replyFuture = replyingKafkaTemplate.sendAndReceive(request);

    try {
      return replyFuture.get();
    } catch (Exception e) {
      throw new NoKafkaResponseException("No response for request: " + input, e);
    }
  }

  private Response<O> getResponseFromStorage(String key) {
    var responseContent =
        datafactoryResponseCephService
            .getContent(datafactoryResponseBucket, key)
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
      datafactoryResponseCephService.deleteObject(datafactoryResponseBucket, key);
    } catch (Exception e) {
      log.error("Exception while deleting processed message from ceph", e);
    }
  }
}
