package com.epam.digital.data.platform.restapi.core.config;

import static com.epam.digital.data.platform.restapi.core.utils.ResponseCode.AUTHENTICATION_FAILED;

import com.epam.digital.data.platform.restapi.core.model.DetailedErrorResponse;
import com.epam.digital.data.platform.restapi.core.service.RestAuditEventsFacade;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import com.epam.digital.data.platform.starter.security.jwt.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

@Component
public class UnauthorizedRequestHandler extends RestAuthenticationEntryPoint {

  private final TraceProvider traceProvider;
  private final ObjectMapper objectMapper;
  private final RestAuditEventsFacade restAuditEventsFacade;

  public UnauthorizedRequestHandler(ObjectMapper objectMapper, TraceProvider traceProvider,
      RestAuditEventsFacade restAuditEventsFacade) {
    super(objectMapper);
    this.traceProvider = traceProvider;
    this.objectMapper = objectMapper;
    this.restAuditEventsFacade = restAuditEventsFacade;
  }

  @Override
  @SuppressWarnings("findsecbugs:XSS_SERVLET")
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException e) throws IOException {
    restAuditEventsFacade.sendExceptionAudit(EventType.SECURITY_EVENT, AUTHENTICATION_FAILED);

    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.getWriter().write(objectMapper
        .writeValueAsString(newDetailedResponse(AUTHENTICATION_FAILED)));
  }

  private <T> DetailedErrorResponse<T> newDetailedResponse(String code) {
    var response = new DetailedErrorResponse<T>();
    response.setTraceId(traceProvider.getRequestId());
    response.setCode(code);
    return response;
  }
}
