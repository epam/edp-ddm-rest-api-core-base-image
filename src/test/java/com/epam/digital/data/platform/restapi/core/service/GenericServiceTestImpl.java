package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.config.properties.KafkaProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

@TestComponent
public class GenericServiceTestImpl extends GenericService<Request<UUID>, MockEntity> {

  public GenericServiceTestImpl(
      ReplyingKafkaTemplate<String, Request<UUID>, String> replyingKafkaTemplate,
      KafkaProperties.Handler topics,
      DigitalSignatureService digitalSignatureService,
      TraceProvider traceProvider,
      CephService datafactoryResponseCephService,
      ObjectMapper objectMapper,
      boolean isEnabled,
      String datafactoryResponseBucket) {
    super(
        replyingKafkaTemplate,
        topics,
        digitalSignatureService,
        traceProvider,
        datafactoryResponseCephService,
        objectMapper,
        isEnabled, datafactoryResponseBucket);
  }

  @Override
  protected TypeReference type() {
    return new TypeReference<Response<MockEntity>>() {};
  }
}
