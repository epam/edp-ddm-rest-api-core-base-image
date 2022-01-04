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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.service.impl.GenericQueryServiceTestImpl;
import com.epam.digital.data.platform.restapi.core.queryhandler.impl.QueryHandlerTestImpl;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = GenericQueryServiceTestImpl.class)
class GenericQueryServiceTest {

  private static final UUID ENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
  private static final String KEY = "datafactory-key";

  @MockBean
  QueryHandlerTestImpl mockQueryHandler;

  @Autowired
  private GenericQueryServiceTestImpl instance;

  @Test
  @DisplayName("Check if response is failed on record not found")
  void notFound() {
    when(mockQueryHandler.findById(any())).thenReturn(Optional.empty());

    var response = instance.request(mockInput());

    assertThat(response.getPayload()).isNull();
    assertThat(response.getStatus()).isEqualTo(Status.NOT_FOUND);
  }

  @Test
  @DisplayName("Check if status is correct if entity found")
  void happyReadPath() {
    MockEntity mock = new MockEntity();
    mock.setConsentId(ENTITY_ID);
    mock.setPersonFullName("stub");
    when(mockQueryHandler.findById(any())).thenReturn(Optional.of(mock));

    Response<MockEntity> reponse = instance.request(mockInput());

    assertThat(reponse.getPayload()).isEqualTo(mock);
    assertThat(reponse.getStatus()).isEqualTo(Status.SUCCESS);
  }

  private Request<UUID> mockInput() {
    return new Request<>(ENTITY_ID, null, null);
  }

  private Request<MockEntity> mockRequest() {
    MockEntity mock = new MockEntity();
    Request<MockEntity> request = new Request<>();

    mock.setConsentId(ENTITY_ID);
    mock.setPersonFullName("stub");
    request.setPayload(mock);

    return request;
  }
}
