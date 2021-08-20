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
  public String getAccessToken() {
    return MDC.get(Header.X_ACCESS_TOKEN.getHeaderName().toLowerCase());
  }
}
