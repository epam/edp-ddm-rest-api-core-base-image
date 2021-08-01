package com.epam.digital.data.platform.restapi.core.interceptor;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.epam.digital.data.platform.starter.actuator.livenessprobe.LivenessStateHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LivenessProbeStateInterceptor implements HandlerInterceptor {

  private LivenessStateHandler livenessStateHandler;

  public LivenessProbeStateInterceptor(LivenessStateHandler livenessStateHandler) {
    this.livenessStateHandler = livenessStateHandler;
  }

  @Override
  public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
      Object handler, @Nullable Exception ex) {
    livenessStateHandler
        .handleResponse(HttpStatus.valueOf(response.getStatus()), HttpStatus::is5xxServerError);
  }
}
