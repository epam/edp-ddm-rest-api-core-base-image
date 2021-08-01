package com.epam.digital.data.platform.restapi.core.filter;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static java.util.List.of;

public abstract class AbstractFilter extends OncePerRequestFilter {

  private static final List<String> skipUrls = of("/openapi", "/swagger-ui", "/v3/api-docs",
      "/actuator");

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    return skipUrls.stream().anyMatch(p -> request.getRequestURI().startsWith(p));
  }
}
