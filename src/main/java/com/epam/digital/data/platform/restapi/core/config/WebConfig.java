package com.epam.digital.data.platform.restapi.core.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private HandlerInterceptor livenessProbeStateInterceptor;

  public WebConfig(HandlerInterceptor livenessProbeStateInterceptor) {
    this.livenessProbeStateInterceptor = livenessProbeStateInterceptor;
  }

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new RequestContextResolver());
    resolvers.add(new SecurityContextResolver());
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry.addInterceptor(livenessProbeStateInterceptor);
  }
}