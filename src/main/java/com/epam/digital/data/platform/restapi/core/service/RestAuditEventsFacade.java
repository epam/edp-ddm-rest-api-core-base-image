package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.service.AbstractAuditFacade;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import java.time.Clock;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestAuditEventsFacade extends AbstractAuditFacade {

  static final String INVALID_ACCESS_TOKEN_EVENT_NAME = "Access Token is not valid";
  static final String INVALID_SIGNATURE_EVENT_NAME = "Invalid signature";
  static final String CODE_JWT_INVALID = "JWT_INVALID";

  private final TraceProvider traceProvider;
  private final TokenParser tokenParser;

  static final String HTTP_REQUEST = "HTTP request. Method: ";
  static final String EXCEPTION = "EXCEPTION";

  static final String ACTION = "action";

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

  public void auditInvalidAccessToken() {
    var event = createBaseAuditEvent(
        EventType.SECURITY_EVENT, INVALID_ACCESS_TOKEN_EVENT_NAME, traceProvider.getRequestId())
        .setBusinessProcessInfo(traceProvider.getSourceSystem(),
            traceProvider.getSourceBusinessProcessInstanceId(), traceProvider.getSourceBusinessProcess());

    event.setContext(Map.of(ACTION, CODE_JWT_INVALID));

    auditService.sendAudit(event.build());
  }

  public void auditSignatureInvalid(String jwt) {
    var event = createBaseAuditEvent(
        EventType.SECURITY_EVENT, INVALID_SIGNATURE_EVENT_NAME, traceProvider.getRequestId())
        .setBusinessProcessInfo(traceProvider.getSourceSystem(),
            traceProvider.getSourceBusinessProcessInstanceId(), traceProvider.getSourceBusinessProcess());

    event.setContext(Map.of(ACTION, "SIGN_BREACH"));
    setUserInfoToEvent(event, jwt);

    auditService.sendAudit(event.build());
  }

  public void sendExceptionAudit(EventType eventType, String action) {
    var event = createBaseAuditEvent(eventType, EXCEPTION, traceProvider.getRequestId())
        .setBusinessProcessInfo(traceProvider.getSourceSystem(),
            traceProvider.getSourceBusinessProcessInstanceId(), traceProvider.getSourceBusinessProcess());

    var context = auditService.createContext(action, null, null, null, null, null);
    event.setContext(context);

    auditService.sendAudit(event.build());
  }

  public void sendRestAudit(EventType eventType, String methodName, String action, String jwt,
      String step, Object id, String result) {
    var event = createBaseAuditEvent(
        eventType, HTTP_REQUEST + methodName, traceProvider.getRequestId())
        .setBusinessProcessInfo(traceProvider.getSourceSystem(),
            traceProvider.getSourceBusinessProcessInstanceId(), traceProvider.getSourceBusinessProcess());

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
