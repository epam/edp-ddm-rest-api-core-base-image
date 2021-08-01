package com.epam.digital.data.platform.restapi.core.filter;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
@Order(FiltersOrder.filterChainExceptionHandler)
public class FilterChainExceptionHandler extends OncePerRequestFilter {

  private final HandlerExceptionResolver resolver;

  public FilterChainExceptionHandler(
      @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver) {
    this.resolver = resolver;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) {

    try {
      filterChain.doFilter(request, response);
    } catch (Exception e) {
      resolver.resolveException(request, response, null, e);
    }
  }
}
