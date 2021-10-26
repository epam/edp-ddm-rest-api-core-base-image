package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.starter.audit.model.AuditUserInfo;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.audit.service.AbstractAuditFacade;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import java.time.Clock;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DatabaseEventsFacade extends AbstractAuditFacade {

  static final String DB_MODIFYING = "DB request. Method: ";

  private final TraceProvider traceProvider;
  private final AuditSourceInfoProvider auditSourceInfoProvider;

  public DatabaseEventsFacade(
      AuditService auditService,
      @Value("${spring.application.name:kafka-api}") String appName,
      Clock clock,
      TraceProvider traceProvider,
      AuditSourceInfoProvider auditSourceInfoProvider) {
    super(auditService, appName, clock);
    this.traceProvider = traceProvider;
    this.auditSourceInfoProvider = auditSourceInfoProvider;
  }

  public void sendDbAudit(
      String methodName,
      String tableName,
      String action,
      JwtClaimsDto userClaims,
      String step,
      String entityId,
      Set<String> fields,
      String result) {
    var event =
        createBaseAuditEvent(
                EventType.USER_ACTION, DB_MODIFYING + methodName, traceProvider.getRequestId())
            .setSourceInfo(auditSourceInfoProvider.getAuditSourceInfo());

    var context = auditService.createContext(action, step, tableName, entityId, fields, result);
    event.setContext(context);

    var userInfo = AuditUserInfo.AuditUserInfoBuilder.anAuditUserInfo()
            .userName(userClaims.getFullName())
            .userKeycloakId(userClaims.getSubject())
            .userDrfo(userClaims.getDrfo())
            .build();
    event.setUserInfo(userInfo);

    auditService.sendAudit(event.build());
  }
}
