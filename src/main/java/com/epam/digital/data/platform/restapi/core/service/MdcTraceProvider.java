package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.restapi.core.utils.Header;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class MdcTraceProvider implements TraceProvider {

  @Override
  public String getRequestId() {
    return MDC.get(Header.TRACE_ID.getHeaderName());
  }

  @Override
  public String getSourceSystem() {
    return MDC.get(Header.X_SOURCE_SYSTEM.getHeaderName().toLowerCase());
  }

  @Override
  public String getSourceBusinessProcessInstanceId() {
    return MDC.get(Header.X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName().toLowerCase());
  }

  @Override
  public String getSourceBusinessProcess() {
    return MDC.get(Header.X_SOURCE_BUSINESS_PROCESS.getHeaderName().toLowerCase());
  }
}
