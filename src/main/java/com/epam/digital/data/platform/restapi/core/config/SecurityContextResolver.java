package com.epam.digital.data.platform.restapi.core.config;

import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.annotation.HttpSecurityContext;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class SecurityContextResolver implements HandlerMethodArgumentResolver {

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterAnnotation(HttpSecurityContext.class) != null;
  }

  @Override
  public Object resolveArgument(
      MethodParameter parameter,
      ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest,
      WebDataBinderFactory binderFactory) {

    return webRequest.getAttribute(SecurityContext.class.getSimpleName(), RequestAttributes.SCOPE_REQUEST);
  }
}
