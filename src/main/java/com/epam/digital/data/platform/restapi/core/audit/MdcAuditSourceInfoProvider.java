package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.restapi.core.utils.Header;
import com.epam.digital.data.platform.starter.audit.model.AuditSourceInfo;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MdcAuditSourceInfoProvider implements AuditSourceInfoProvider {

  @Override
  public AuditSourceInfo getAuditSourceInfo() {
    return AuditSourceInfo.AuditSourceInfoBuilder.anAuditSourceInfo()
        .system(MDC.get(Header.X_SOURCE_SYSTEM.getHeaderName().toLowerCase()))
        .application(MDC.get(Header.X_SOURCE_APPLICATION.getHeaderName().toLowerCase()))
        .businessProcess(
            MDC.get(Header.X_SOURCE_BUSINESS_PROCESS.getHeaderName().toLowerCase()))
        .businessProcessDefinitionId(
            MDC.get(Header.X_SOURCE_BUSINESS_PROCESS_DEFINITION_ID.getHeaderName().toLowerCase()))
        .businessProcessInstanceId(
            MDC.get(Header.X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName().toLowerCase()))
        .businessActivity(
            MDC.get(Header.X_SOURCE_BUSINESS_ACTIVITY.getHeaderName().toLowerCase()))
        .businessActivityInstanceId(
            MDC.get(Header.X_SOURCE_BUSINESS_ACTIVITY_INSTANCE_ID.getHeaderName().toLowerCase()))
        .build();
  }
}
