package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.restapi.core.audit.AuditSourceInfoProvider;
import com.epam.digital.data.platform.restapi.core.audit.MdcAuditSourceInfoProvider;
import com.epam.digital.data.platform.restapi.core.utils.Header;
import com.epam.digital.data.platform.starter.audit.model.AuditSourceInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class MdcAuditSourceInfoProviderTest {

  private static final String SOURCE_SYSTEM = "system";
  private static final String SOURCE_APPLICATION = "source_app";
  private static final String BUSINESS_PROCESS = "bp";
  private static final String BUSINESS_PROCESS_DEFINITION_ID = "bp_def_id";
  private static final String BUSINESS_PROCESS_INSTANCE_ID = "bp_id";
  private static final String BUSINESS_ACTIVITY = "act";
  private static final String BUSINESS_ACTIVITY_INSTANCE_ID = "bp_act";

  private final AuditSourceInfoProvider auditSourceInfoProvider = new MdcAuditSourceInfoProvider();

  @Test
  void expectCorrectAuditSourceInfoRetrievedFromMdc() {
    MDC.put(Header.X_SOURCE_SYSTEM.getHeaderName().toLowerCase(), SOURCE_SYSTEM);
    MDC.put(Header.X_SOURCE_APPLICATION.getHeaderName().toLowerCase(), SOURCE_APPLICATION);
    MDC.put(Header.X_SOURCE_BUSINESS_PROCESS.getHeaderName().toLowerCase(), BUSINESS_PROCESS);
    MDC.put(
        Header.X_SOURCE_BUSINESS_PROCESS_DEFINITION_ID.getHeaderName().toLowerCase(),
        BUSINESS_PROCESS_DEFINITION_ID);
    MDC.put(
        Header.X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName().toLowerCase(),
        BUSINESS_PROCESS_INSTANCE_ID);
    MDC.put(Header.X_SOURCE_BUSINESS_ACTIVITY.getHeaderName().toLowerCase(), BUSINESS_ACTIVITY);
    MDC.put(
        Header.X_SOURCE_BUSINESS_ACTIVITY_INSTANCE_ID.getHeaderName().toLowerCase(),
        BUSINESS_ACTIVITY_INSTANCE_ID);

    var actualSourceInfo = auditSourceInfoProvider.getAuditSourceInfo();

    var expectedSourceInfo =
        AuditSourceInfo.AuditSourceInfoBuilder.anAuditSourceInfo()
            .system(SOURCE_SYSTEM)
            .application(SOURCE_APPLICATION)
            .businessProcess(BUSINESS_PROCESS)
            .businessProcessDefinitionId(BUSINESS_PROCESS_DEFINITION_ID)
            .businessProcessInstanceId(BUSINESS_PROCESS_INSTANCE_ID)
            .businessActivity(BUSINESS_ACTIVITY)
            .businessActivityInstanceId(BUSINESS_ACTIVITY_INSTANCE_ID)
            .build();

    assertThat(actualSourceInfo).usingRecursiveComparison().isEqualTo(expectedSourceInfo);
  }

  @AfterEach
  void afterEach() {
    MDC.clear();
  }
}