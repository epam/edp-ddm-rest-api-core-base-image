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

import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.starter.kafka.config.properties.KafkaProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

@TestComponent
public class GenericServiceTestImpl extends GenericService<Request<UUID>, MockEntity> {

  public GenericServiceTestImpl(
      ReplyingKafkaTemplate<String, Request<UUID>, String> replyingKafkaTemplate,
      KafkaProperties.RequestReplyHandler topics,
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
