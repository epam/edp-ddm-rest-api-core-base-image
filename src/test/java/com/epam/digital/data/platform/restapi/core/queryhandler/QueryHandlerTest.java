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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.config.JooqTestConfig;
import com.epam.digital.data.platform.restapi.core.config.TestDataProvider;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.exception.ForbiddenOperationException;
import com.epam.digital.data.platform.restapi.core.exception.SqlErrorException;
import com.epam.digital.data.platform.restapi.core.model.FieldsAccessCheckDto;
import com.epam.digital.data.platform.restapi.core.queryhandler.impl.QueryHandlerTestImpl;
import com.epam.digital.data.platform.restapi.core.service.AccessPermissionService;
import com.epam.digital.data.platform.restapi.core.service.JwtInfoProvider;
import com.epam.digital.data.platform.restapi.core.tabledata.TableDataProvider;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(classes = QueryHandlerTestImpl.class)
@ContextConfiguration(classes = JooqTestConfig.class)
class QueryHandlerTest {

  @MockBean
  private AccessPermissionService accessPermissionService;
  @MockBean
  private JwtInfoProvider jwtInfoProvider;

  @MockBean
  private TableDataProvider tableDataProvider;
  @Autowired
  private QueryHandlerTestImpl queryHandler;

  @Test
  @DisplayName("Successful find one record by id test")
  void expectSuccessfulFindByIdWhenHaveValidRoles() {
    JwtClaimsDto userClaims = new JwtClaimsDto();
    when(jwtInfoProvider.getUserClaims(any())).thenReturn(userClaims);
    when(accessPermissionService.hasReadAccess(any(), any())).thenReturn(true);
    when(tableDataProvider.tableName()).thenReturn("table");
    when(tableDataProvider.pkColumnName()).thenReturn("id");
    Request<UUID> input = getMockRequest(TestDataProvider.ENTITY_ID);
    final Optional<MockEntity> consentSubject = queryHandler.findById(input);
    MockEntity record = consentSubject.orElse(new MockEntity());

    ArgumentCaptor<List<FieldsAccessCheckDto>> captor = ArgumentCaptor.forClass(List.class);
    verify(accessPermissionService).hasReadAccess(captor.capture(), eq(userClaims));
    assertThat(captor.getValue().get(0).getTableName()).isEqualTo("table");
    assertThat(captor.getValue().get(0).getFields())
        .containsExactlyInAnyOrder("consent_id", "person_full_name", "person_pass_number");
    Assertions.assertAll(
        "A",
        () -> assertEquals(TestDataProvider.ENTITY_ID, record.getConsentId()),
        () -> assertEquals("Roman", record.getPersonFullName()),
        () -> assertEquals("АА000000", record.getPersonPassNumber()));
  }

  @Test
  void expectExceptionWhenNoAccessForFindById() {
    JwtClaimsDto userClaims = new JwtClaimsDto();
    when(jwtInfoProvider.getUserClaims(any())).thenReturn(userClaims);
    when(accessPermissionService.hasReadAccess(any(), any())).thenReturn(false);
    Request<UUID> input = getMockRequest(TestDataProvider.ENTITY_ID);

    ForbiddenOperationException e =
        assertThrows(ForbiddenOperationException.class, () -> queryHandler.findById(input));
    assertThat(e.getKafkaResponseStatus()).isEqualTo(Status.FORBIDDEN_OPERATION);
    assertThat(e.getDetails()).isNull();
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnSearchMethod() {
    when(accessPermissionService.hasReadAccess(any(), any())).thenReturn(true);
//    queryHandler = new QueryHandlerTestImpl(tableDataProvider);
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
