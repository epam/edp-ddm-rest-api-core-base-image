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

import static com.epam.digital.data.platform.restapi.core.service.GenericService.DIGITAL_SEAL_KAFKA_HEADER;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.integration.ceph.exception.MisconfigurationException;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.ResponseHeaders;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.exception.KafkaCephResponseNotFoundException;
import com.epam.digital.data.platform.restapi.core.exception.NoKafkaResponseException;
import com.epam.digital.data.platform.starter.kafka.config.properties.KafkaProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;

@ExtendWith(MockitoExtension.class)
class GenericServiceTest {

  static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
  static final String CEPH_RESPONSE_KEY = "key";
  static final String BUCKET_NAME = "bucket";

  GenericServiceTestImpl instance;

  @Mock
  ReplyingKafkaTemplate<String, Request<UUID>, String> replyingKafkaTemplate;
  @Mock
  DigitalSignatureService digitalSignatureService;
  @Mock
  TraceProvider traceProvider;
  @Mock
  CephService cephService;

  ObjectMapper objectMapper = new ObjectMapper();

  @Captor
  ArgumentCaptor<ProducerRecord<String, Request<UUID>>> captor;

  KafkaProperties.RequestReplyHandler topics;

  @BeforeEach
  void setUp() {
    topics = new KafkaProperties.RequestReplyHandler();
    topics.setRequest("request-topic");
    topics.setReply("replay-topic");

    instance = new GenericServiceTestImpl(replyingKafkaTemplate, topics,
        digitalSignatureService, traceProvider, cephService, objectMapper, false, BUCKET_NAME);
  }

  @Nested
  class GenericFlow {

    @Test
    void shouldSendRequest() throws JsonProcessingException {
      // given
      String expected = "Some Name";

      MockEntity entity = new MockEntity();
      entity.setPersonFullName(expected);

      RequestReplyFuture<String, Request<UUID>, String> replyFuture =
          wrapResponseObjectAsKafkaReplay(new Request<>(ID, null, null), entity);
      when(replyingKafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenReturn(replyFuture);

      // when
      Response<MockEntity> response = instance.request(new Request<>(ID, null, null));

      // then
      assertThat(response.getPayload().getPersonFullName()).isEqualTo(expected);
    }

    @Test
    void shouldThrowExceptionWhenTimeout()
        throws ExecutionException, InterruptedException, TimeoutException {
      RequestReplyFuture mockReplyFuture = Mockito.mock(RequestReplyFuture.class);
      when(replyingKafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenReturn(mockReplyFuture);

      Mockito.doThrow(new InterruptedException()).when(mockReplyFuture).get(30L, SECONDS);

      Exception exception = assertThrows(NoKafkaResponseException.class, () -> {
        instance.request(new Request<>(ID, null, null));
      });

      assertThat(exception.getCause()).isInstanceOf(InterruptedException.class);
    }

    @Test
    void shouldThrowExceptionWhenInvalidJson() throws ExecutionException, InterruptedException {
      Request<UUID> request = new Request<>(ID, null, null);

      RequestReplyFuture<String, Request<UUID>, String>
          replyFuture =
          wrapResponseObjectAsKafkaReplay(request, "invalid json");
      when(replyingKafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenReturn(replyFuture);

      Exception exception = assertThrows(RuntimeJsonMappingException.class, () -> {
        instance.request(request);
      });
    }
  }

  @Nested
  class LargeResponse {

    @Test
    void expectPayloadFromCephIfResponseCephKeyIsNotNull() throws JsonProcessingException {
      // given
      String expected = "Some Name";

      MockEntity entity = new MockEntity();
      entity.setPersonFullName(expected);

      RequestReplyFuture<String, Request<UUID>, String> replyFuture =
          wrapResponseWithCephHeaderAsKafkaReplay();

      when(replyingKafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenReturn(replyFuture);
      String cephContent =
          "{\"payload\":{\"personFullName\":\"" + expected + "\"}, \"status\":\"SUCCESS\"}";
      when(cephService.getAsString(BUCKET_NAME, CEPH_RESPONSE_KEY))
          .thenReturn(Optional.of(cephContent));

      // when
      Response<MockEntity> response = instance.request(new Request<>(ID, null, null));

      // then
      verify(cephService).getAsString(BUCKET_NAME, CEPH_RESPONSE_KEY);
      assertThat(response.getPayload().getPersonFullName()).isEqualTo(expected);
      assertThat(response.getStatus()).isEqualTo(Status.SUCCESS);

      verify(cephService).delete(BUCKET_NAME, Collections.singleton(CEPH_RESPONSE_KEY));
    }

    @Test
    void expectNoExceptionThrownIfCannotDeleteFromCeph() throws JsonProcessingException {
      // given
      RequestReplyFuture<String, Request<UUID>, String> replyFuture =
          wrapResponseWithCephHeaderAsKafkaReplay();
      when(replyingKafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenReturn(replyFuture);

      String cephContent = "{\"status\":\"SUCCESS\"}";
      when(cephService.getAsString(BUCKET_NAME, CEPH_RESPONSE_KEY))
          .thenReturn(Optional.of(cephContent));

      doThrow(new MisconfigurationException(""))
          .when(cephService)
          .delete(BUCKET_NAME, Collections.singleton(CEPH_RESPONSE_KEY));

      // when
      assertDoesNotThrow(() -> instance.request(new Request<>(ID, null, null)));

      // then
      verify(cephService).delete(BUCKET_NAME, Collections.singleton(CEPH_RESPONSE_KEY));
    }

    @Test
    void expectExceptionThrownIfNotFoundInCephCeph() throws JsonProcessingException {
      RequestReplyFuture<String, Request<UUID>, String> replyFuture =
          wrapResponseWithCephHeaderAsKafkaReplay();
      when(replyingKafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenReturn(replyFuture);

      when(cephService.getAsString(BUCKET_NAME, CEPH_RESPONSE_KEY)).thenReturn(Optional.empty());

      assertThrows(KafkaCephResponseNotFoundException.class,
          () -> instance.request(new Request<>(ID, null, null)));
    }
  }

  @Nested
  class Signature {

    @Test
    void shouldSignPayload() {
      // given
      instance = new GenericServiceTestImpl(replyingKafkaTemplate, topics,
          digitalSignatureService, traceProvider, cephService, objectMapper, true, BUCKET_NAME);
      String expected = "signature";

      RequestReplyFuture<String, Request<UUID>, String> replyFuture =
          wrapResponseObjectAsKafkaReplay(new Request<>(ID, null, null), new MockEntity());
      when(replyingKafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenReturn(replyFuture);

      when(digitalSignatureService.store(any())).thenReturn(expected);

      // when
      instance.request(new Request<>(ID, null, null));

      // then
      verify(digitalSignatureService).sign(any());
      verify(digitalSignatureService).store(any());
    }

    @Test
    void shouldAddSignatureHeader() {
      // given
      instance = new GenericServiceTestImpl(replyingKafkaTemplate, topics,
          digitalSignatureService, traceProvider, cephService, objectMapper, true, BUCKET_NAME);

      String expected = "signature";

      RequestReplyFuture<String, Request<UUID>, String> replyFuture =
          wrapResponseObjectAsKafkaReplay(new Request<>(ID, null, null), new MockEntity());
      when(replyingKafkaTemplate.sendAndReceive(any(ProducerRecord.class))).thenReturn(replyFuture);

      when(digitalSignatureService.store(any())).thenReturn(expected);

      // when
      instance.request(new Request<>(ID, null, null));

      // then
      verify(replyingKafkaTemplate).sendAndReceive(captor.capture());
      assertThat(captor.getValue().headers().lastHeader(DIGITAL_SEAL_KAFKA_HEADER).value())
          .isEqualTo(expected.getBytes());
    }
  }

  private <I, O> RequestReplyFuture<String, I, String> wrapResponseObjectAsKafkaReplay(I input,
      O output) {
    return wrapResponseObjectAsKafkaReplayWithStatus(input, output, Status.SUCCESS);
  }

  private <I, O> RequestReplyFuture<String, I, String> wrapResponseObjectAsKafkaReplayWithStatus(
      I input, O output, Status status) {
    Response<O> responseWrapper = new Response<>();
    responseWrapper.setPayload(output);
    responseWrapper.setStatus(status);

    ConsumerRecord<String, String> responseRecord =
        new ConsumerRecord<>("out", 0, 0, null, toJsonStr(responseWrapper));

    RequestReplyFuture<String, I, String> replyFuture = new RequestReplyFuture<>();
    replyFuture.set(responseRecord);
    return replyFuture;
  }

  private <O> String toJsonStr(O obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private <I, O> RequestReplyFuture<String, I, String> wrapResponseWithCephHeaderAsKafkaReplay() {
    RequestReplyFuture<String, I, String> replyFuture = new RequestReplyFuture<>();
    Response<O> responseWrapper = new Response<>();

    ConsumerRecord<String, String> responseRecord =
            new ConsumerRecord<>(
                    "out",
                    0,
                    0,
                    0,
                    null,
                    null,
                    0,
                    0,
                    null,
                    toJsonStr(responseWrapper),
                    new RecordHeaders(
                            List.of(
                                    new RecordHeader(
                                            ResponseHeaders.CEPH_RESPONSE_KEY, CEPH_RESPONSE_KEY.getBytes()))));
    replyFuture.set(responseRecord);
    return replyFuture;
  }
}
