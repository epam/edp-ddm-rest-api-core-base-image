package com.epam.digital.data.platform.restapi.core.config;

import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.restapi.core.annotation.HttpRequestContext;
import com.epam.digital.data.platform.restapi.core.utils.Header;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class RequestContextResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterAnnotation(HttpRequestContext.class) != null;
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    RequestContext context = new RequestContext();
    context.setSystem(webRequest.getHeader(Header.X_SOURCE_SYSTEM.getHeaderName()));
    context.setApplication(
        webRequest.getHeader(Header.X_SOURCE_APPLICATION.getHeaderName()));
    context.setBusinessProcess(
        webRequest.getHeader(Header.X_SOURCE_BUSINESS_PROCESS.getHeaderName()));
    context.setBusinessProcessDefinitionId(
        webRequest.getHeader(Header.X_SOURCE_BUSINESS_PROCESS_DEFINITION_ID.getHeaderName()));
    context.setBusinessProcessInstanceId(
        webRequest.getHeader(Header.X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName()));

    context.setBusinessActivity(
        webRequest.getHeader(Header.X_SOURCE_BUSINESS_ACTIVITY.getHeaderName()));
    context.setBusinessActivityInstanceId(
        webRequest.getHeader(Header.X_SOURCE_BUSINESS_ACTIVITY_INSTANCE_ID.getHeaderName()));

    return context;
  }
}
