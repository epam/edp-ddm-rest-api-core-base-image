package com.epam.digital.data.platform.restapi.core.config;

import com.epam.digital.data.platform.starter.security.jwt.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Component
public class UnauthorizedRequestHandler extends RestAuthenticationEntryPoint {

  private final HandlerExceptionResolver handlerExceptionResolver;

  public UnauthorizedRequestHandler(ObjectMapper objectMapper, HandlerExceptionResolver handlerExceptionResolver) {
    super(objectMapper);
    this.handlerExceptionResolver = handlerExceptionResolver;
  }

  @Override
  @SuppressWarnings("findsecbugs:XSS_SERVLET")
  public void commence(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException e) {
    handlerExceptionResolver.resolveException(request, response, null, e);
  }
}
