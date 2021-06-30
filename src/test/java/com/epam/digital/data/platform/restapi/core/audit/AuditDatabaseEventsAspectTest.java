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

package com.epam.digital.data.platform.restapi.core.audit;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.config.JooqTestConfig;
import com.epam.digital.data.platform.restapi.core.converter.EntityConverter;
import com.epam.digital.data.platform.restapi.core.exception.ForbiddenOperationException;
import com.epam.digital.data.platform.restapi.core.exception.SqlErrorException;
import com.epam.digital.data.platform.restapi.core.searchhandler.AbstractSearchHandlerTestImpl;
import com.epam.digital.data.platform.restapi.core.queryhandler.impl.QueryHandlerTestImpl;
import com.epam.digital.data.platform.restapi.core.service.AccessPermissionService;
import com.epam.digital.data.platform.restapi.core.service.JwtInfoProvider;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import com.epam.digital.data.platform.starter.security.dto.RolesDto;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

@Import({AopAutoConfiguration.class})
@SpringBootTest(
    classes = {
        DatabaseAuditAspect.class,
        DatabaseAuditProcessor.class,
        QueryHandlerTestImpl.class,
        AbstractSearchHandlerTestImpl.class
    })
@MockBean(JwtInfoProvider.class)
@MockBean(EntityConverter.class)
@ContextConfiguration(classes = JooqTestConfig.class)
class AuditDatabaseEventsAspectTest {

  private static final String ENTITY_ID = "123e4567-e89b-12d3-a456-426655440000";
  private static String ACCESS_TOKEN;

  @Autowired
  private QueryHandlerTestImpl abstractQueryHandler;
  @Autowired
  private AbstractSearchHandlerTestImpl abstractSearchHandlerTest;

  @MockBean
  private AccessPermissionService accessPermissionService;
  @MockBean
  private DatabaseEventsFacade databaseEventsFacade;
  @MockBean
  private DataSource dataSource;

  @Mock
  private ResultSet resultSet;
  @Mock
  private Connection connection;
  @Mock
  private CallableStatement callableStatement;

  @BeforeAll
  static void init() throws IOException {
    ACCESS_TOKEN = new String(ByteStreams.toByteArray(
        AuditDatabaseEventsAspectTest.class.getResourceAsStream("/accessToken.json")));
  }

  @BeforeEach
  void beforeEach() throws SQLException {
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.prepareCall(any())).thenReturn(callableStatement);
    when(callableStatement.executeQuery()).thenReturn(resultSet);
    when(resultSet.next()).thenReturn(true);
  }

  @Test
  void expectAuditAspectBeforeAndAfterFindByIdMethodWhenNoException() {
    when(accessPermissionService.hasReadAccess(any(), any())).thenReturn(true);
    abstractQueryHandler.findById(mockRequest(ACCESS_TOKEN, ENTITY_ID));

    verify(databaseEventsFacade, times(2))
        .sendDbAudit(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnFindByIdMethod() {
    assertThrows(
        ForbiddenOperationException.class,
        () -> abstractQueryHandler.findById(mockRequest(ACCESS_TOKEN, ENTITY_ID)));

    verify(databaseEventsFacade)
        .sendDbAudit(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @DirtiesContext
  void expectAuditAspectBeforeAndAfterSearchMethodWhenNoException() {
    abstractSearchHandlerTest.search(mockRequest(null, null));

    verify(databaseEventsFacade, times(2))
        .sendDbAudit(any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  @DirtiesContext
  void expectAuditAspectOnlyBeforeWhenExceptionOnSearchMethod() {
    abstractSearchHandlerTest.setTableName(null);
    var input = mockRequest(null, null);

    assertThrows(
        SqlErrorException.class, () -> abstractSearchHandlerTest.search(input));

    verify(databaseEventsFacade)
        .sendDbAudit(any(), any(), any(), any(), any(), any(), any(), any());
  }

  private JwtClaimsDto mockJwtClaimsDto() {
    JwtClaimsDto result = new JwtClaimsDto();
    result.setRoles(List.of("user", "manager"));

    RolesDto realmAccess = new RolesDto();
    realmAccess.setRoles(List.of("admin"));
    result.setRealmAccess(realmAccess);

    result.setDrfo("1010101014");
    result.setFullName("Сидоренко Василь Леонідович");
    return result;
  }

  private <T> Request mockRequest(String jwt, T payload) {
    SecurityContext sc = new SecurityContext(jwt, null, null);
    return new Request(payload, null, sc);
  }
}