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

import static com.epam.digital.data.platform.restapi.core.audit.AuditEventUtils.ACTION;
import static com.epam.digital.data.platform.restapi.core.audit.AuditEventUtils.APP_NAME;
import static com.epam.digital.data.platform.restapi.core.audit.AuditEventUtils.CURR_TIME;
import static com.epam.digital.data.platform.restapi.core.audit.AuditEventUtils.METHOD_NAME;
import static com.epam.digital.data.platform.restapi.core.audit.AuditEventUtils.REQUEST_ID;
import static com.epam.digital.data.platform.restapi.core.audit.AuditEventUtils.RESULT;
import static com.epam.digital.data.platform.restapi.core.audit.AuditEventUtils.STEP;
import static com.epam.digital.data.platform.restapi.core.audit.AuditEventUtils.createSourceInfo;
import static com.epam.digital.data.platform.restapi.core.audit.AuditEventUtils.createUserInfo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.starter.audit.model.AuditEvent;
import com.epam.digital.data.platform.starter.audit.model.AuditSourceInfo;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.time.Clock;
import java.time.ZoneId;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DatabaseEventsFacadeTest {

  private static final String TABLE_NAME = "table";
  private static final Set<String> FIELDS = Set.of("first", "second");
  private static JwtClaimsDto userClaims;

  private DatabaseEventsFacade databaseEventsFacade;

  @Mock
  private AuditService auditService;
  @Mock
  private AuditSourceInfoProvider auditSourceInfoProvider;
  @Mock
  private TraceProvider traceProvider;

  private final Clock clock =
      Clock.fixed(CURR_TIME.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());

  private AuditSourceInfo mockSourceInfo;

  @BeforeAll
  static void init() throws IOException {
    TokenParser tokenParser = new TokenParser(new ObjectMapper());
    String accessToken = new String(ByteStreams.toByteArray(
            DatabaseEventsFacadeTest.class.getResourceAsStream("/accessToken.json")));
    userClaims = tokenParser.parseClaims(accessToken);
  }

  @BeforeEach
  void beforeEach() {
    databaseEventsFacade =
        new DatabaseEventsFacade(
            auditService, APP_NAME, clock, traceProvider, auditSourceInfoProvider);

    when(traceProvider.getRequestId()).thenReturn(REQUEST_ID);

    mockSourceInfo = createSourceInfo();
    when(auditSourceInfoProvider.getAuditSourceInfo())
            .thenReturn(mockSourceInfo);
  }

  @Test
  void expectCorrectAuditEvent() {
    Map<String, Object> context = Map.of(
        "action", ACTION,
        "step", STEP,
        "tablename", TABLE_NAME,
        "row_id", "42",
        "fields", FIELDS,
        "result", RESULT
    );
    when(auditService.createContext(ACTION, STEP, TABLE_NAME, "42", FIELDS, RESULT))
        .thenReturn(context);

    databaseEventsFacade
        .sendDbAudit(METHOD_NAME, TABLE_NAME, ACTION, userClaims, STEP, "42", FIELDS, RESULT);

    ArgumentCaptor<AuditEvent> auditEventCaptor = ArgumentCaptor.forClass(AuditEvent.class);

    verify(auditService).sendAudit(auditEventCaptor.capture());
    AuditEvent actualEvent = auditEventCaptor.getValue();

    var expectedEvent = AuditEvent.AuditEventBuilder.anAuditEvent()
            .application(APP_NAME)
            .name("DB request. Method: method")
            .requestId(REQUEST_ID)
            .sourceInfo(mockSourceInfo)
            .userInfo(createUserInfo())
            .currentTime(clock.millis())
            .eventType(EventType.USER_ACTION)
            .context(context)
            .build();

    assertThat(actualEvent).usingRecursiveComparison().isEqualTo(expectedEvent);
  }
}
