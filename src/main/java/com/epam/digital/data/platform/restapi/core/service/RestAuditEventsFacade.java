package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.restapi.core.model.audit.ExceptionAuditEvent;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.service.AbstractAuditFacade;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestAuditEventsFacade extends AbstractAuditFacade {

  static final String INVALID_ACCESS_TOKEN_EVENT_NAME = "Access Token is not valid";
  static final String INVALID_SIGNATURE_EVENT_NAME = "Invalid signature";

  private final TraceProvider traceProvider;
  private final TokenParser tokenParser;

  static final String HTTP_REQUEST = "HTTP request. Method: ";
  static final String EXCEPTION = "EXCEPTION";

  public RestAuditEventsFacade(
      @Value("${spring.application.name:rest-api}") String appName,
      AuditService auditService,
      TraceProvider traceProvider,
      Clock clock,
      TokenParser tokenParser) {
    super(appName, auditService, clock);
    this.traceProvider = traceProvider;
    this.tokenParser = tokenParser;
  }

  public void sendExceptionAudit(ExceptionAuditEvent exceptionAuditEvent) {
    var event =
        createBaseAuditEvent(
            exceptionAuditEvent.getEventType(), EXCEPTION, traceProvider.getRequestId())
            .setBusinessProcessInfo(
                traceProvider.getSourceSystem(),
                traceProvider.getSourceBusinessProcessInstanceId(),
                traceProvider.getSourceBusinessProcess());

    var context =
        auditService.createContext(exceptionAuditEvent.getAction(), null, null, null, null, null);
    event.setContext(context);
    if (exceptionAuditEvent.isUserInfoEnabled()) {
      setUserInfoToEvent(event, traceProvider.getAccessToken());
    }
    auditService.sendAudit(event.build());
  }

  public void sendRestAudit(EventType eventType, String methodName, String action, String jwt,
      String step, Object id, String result) {
    var event = createBaseAuditEvent(
        eventType, HTTP_REQUEST + methodName, traceProvider.getRequestId())
        .setBusinessProcessInfo(traceProvider.getSourceSystem(),
            traceProvider.getSourceBusinessProcessInstanceId(),
            traceProvider.getSourceBusinessProcess());

    var entityId = (id != null) ? id.toString() : null;
    var context = auditService.createContext(action, step, null, entityId, null, result);
    event.setContext(context);
    setUserInfoToEvent(event, jwt);

    auditService.sendAudit(event.build());
  }

  private void setUserInfoToEvent(GroupedAuditEventBuilder event, String jwt) {
    if (jwt == null) {
      return;
    }

    var jwtClaimsDto = tokenParser.parseClaims(jwt);
    event.setUserInfo(jwtClaimsDto.getDrfo(), jwtClaimsDto.getFullName());
  }
}
