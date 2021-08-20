package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.restapi.core.model.audit.ExceptionAuditEvent;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.starter.audit.model.AuditUserInfo;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.service.AbstractAuditFacade;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestAuditEventsFacade extends AbstractAuditFacade {

  private final TraceProvider traceProvider;
  private final AuditSourceInfoProvider auditSourceInfoProvider;
  private final TokenParser tokenParser;

  static final String HTTP_REQUEST = "HTTP request. Method: ";
  static final String EXCEPTION = "EXCEPTION";

  public RestAuditEventsFacade(
      AuditService auditService,
      @Value("${spring.application.name:rest-api}") String appName,
      Clock clock,
      TraceProvider traceProvider,
      AuditSourceInfoProvider auditSourceInfoProvider,
      TokenParser tokenParser) {
    super(auditService, appName, clock);
    this.traceProvider = traceProvider;
    this.auditSourceInfoProvider = auditSourceInfoProvider;
    this.tokenParser = tokenParser;
  }

  public void sendExceptionAudit(ExceptionAuditEvent exceptionAuditEvent) {
    var event =
        createBaseAuditEvent(
                exceptionAuditEvent.getEventType(), EXCEPTION, traceProvider.getRequestId())
            .setSourceInfo(auditSourceInfoProvider.getAuditSourceInfo());

    var context =
        auditService.createContext(exceptionAuditEvent.getAction(), null, null, null, null, null);
    event.setContext(context);
    if (exceptionAuditEvent.isUserInfoEnabled()) {
      setUserInfoToEvent(event, traceProvider.getAccessToken());
    }
    auditService.sendAudit(event.build());
  }

  public void sendRestAudit(
      EventType eventType,
      String methodName,
      String action,
      String jwt,
      String step,
      Object id,
      String result) {
    var event =
        createBaseAuditEvent(eventType, HTTP_REQUEST + methodName, traceProvider.getRequestId())
            .setSourceInfo(auditSourceInfoProvider.getAuditSourceInfo());

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
    var userInfo = AuditUserInfo.AuditUserInfoBuilder.anAuditUserInfo()
            .userName(jwtClaimsDto.getFullName())
            .userKeycloakId(jwtClaimsDto.getSubject())
            .userDrfo(jwtClaimsDto.getDrfo())
            .build();
    event.setUserInfo(userInfo);
  }
}