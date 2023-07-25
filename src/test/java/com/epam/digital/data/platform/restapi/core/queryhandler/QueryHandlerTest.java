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

package com.epam.digital.data.platform.restapi.core.queryhandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.config.JooqTestConfig;
import com.epam.digital.data.platform.restapi.core.config.TestDataProvider;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.exception.SqlErrorException;
import com.epam.digital.data.platform.restapi.core.queryhandler.impl.QueryHandlerTestImpl;
import com.epam.digital.data.platform.restapi.core.service.JwtInfoProvider;
import com.epam.digital.data.platform.restapi.core.tabledata.TableDataProvider;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(classes = QueryHandlerTestImpl.class)
@ContextConfiguration(classes = JooqTestConfig.class)
class QueryHandlerTest {

  @MockBean
  private JwtInfoProvider jwtInfoProvider;

  @MockBean
  private TableDataProvider tableDataProvider;
  @Autowired
  private QueryHandlerTestImpl queryHandler;

  @Test
  @DisplayName("Successful find one record by id test")
  void expectSuccessfulFindById() {
    JwtClaimsDto userClaims = new JwtClaimsDto();
    when(jwtInfoProvider.getUserClaims(any())).thenReturn(userClaims);
    when(tableDataProvider.tableName()).thenReturn("table");
    when(tableDataProvider.pkColumnName()).thenReturn("id");
    Request<UUID> input = getMockRequest(TestDataProvider.ENTITY_ID);
    final Optional<MockEntity> consentSubject = queryHandler.findById(input);
    MockEntity record = consentSubject.orElse(new MockEntity());

    Assertions.assertAll(
        "A",
        () -> assertEquals(TestDataProvider.ENTITY_ID, record.getConsentId()),
        () -> assertEquals("Roman", record.getPersonFullName()),
        () -> assertEquals("АА000000", record.getPersonPassNumber()));
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnSearchMethod() {
    ReflectionTestUtils.setField(queryHandler, "jwtInfoProvider", jwtInfoProvider);
    var input = new Request<UUID>();

    assertThrows(SqlErrorException.class, () -> queryHandler.findById(input));
  }

  private <T> Request<T> getMockRequest(T payload) {
    Request<T> input = new Request<>();
    input.setPayload(payload);
    return input;
  }
}
