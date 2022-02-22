/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.exception.AuditException;
import com.epam.digital.data.platform.restapi.core.model.DetailedErrorResponse;
import com.epam.digital.data.platform.restapi.core.model.audit.ExceptionAuditEvent;
import com.epam.digital.data.platform.restapi.core.utils.ResponseCode;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;

@Aspect
@Component
public class ControllerAuditAspect {

  private static final Set<Integer> httpStatusOfSecurityAudit = Set.of(401, 403, 412);
  private static final Set<String> responseCodeOfSecurityAudit = Set.of(
      ResponseCode.AUTHENTICATION_FAILED,
      ResponseCode.SIGNATURE_VIOLATION,
      ResponseCode.JWT_INVALID,
      ResponseCode.JWT_EXPIRED,
      ResponseCode.FORBIDDEN_OPERATION
  );

  private static final Set<Class<? extends Annotation>> httpAnnotations = Set.of(
      GetMapping.class,
      PostMapping.class,
      PutMapping.class,
      PatchMapping.class,
      DeleteMapping.class);

  // action
  static final String CREATE = "CREATE ENTITY";
  static final String READ = "READ ENTITY";
  static final String UPDATE = "UPDATE ENTITY";
  static final String UPSERT = "UPSERT ENTITY";
  static final String DELETE = "DELETE ENTITY";
  static final String SEARCH = "SEARCH ENTITY";

  // step
  static final String BEFORE = "BEFORE";
  static final String AFTER = "AFTER";

  private final RestAuditEventsFacade restAuditEventsFacade;

  public ControllerAuditAspect(RestAuditEventsFacade restAuditEventsFacade) {
    this.restAuditEventsFacade = restAuditEventsFacade;
  }

  @Pointcut("@annotation(com.epam.digital.data.platform.restapi.core.audit.AuditableException)")
  public void auditableExceptionPointcut() {
  }

  @Pointcut("@annotation(com.epam.digital.data.platform.restapi.core.audit.AuditableController)")
  public void controller() {
  }

  @AfterReturning(pointcut = "auditableExceptionPointcut()", returning = "response")
  void exceptionAudit(JoinPoint joinPoint, ResponseEntity<?> response) {
    var auditableExceptionAnnotation = ((MethodSignature) joinPoint.getSignature())
        .getMethod()
        .getAnnotation(AuditableException.class);
    prepareAndSendExceptionAudit(response, auditableExceptionAnnotation);
  }

  @Around("controller() && args(object, context, securityContext)")
  Object auditGetPostDeletePutUpsert(ProceedingJoinPoint joinPoint, Object object, RequestContext context,
      SecurityContext securityContext) throws Throwable {

    var annotation = getAnnotation(joinPoint);

    if (annotation.equals(GetMapping.class) && object instanceof UUID) {
      return prepareAndSendRestAudit(joinPoint, READ, (UUID) object, securityContext);
    } else if (annotation.equals(GetMapping.class)) {
      return prepareAndSendRestAudit(joinPoint, SEARCH, null, securityContext);
    } else if (annotation.equals(PostMapping.class)) {
      return prepareAndSendRestAudit(joinPoint, CREATE, null, securityContext);
    } else if (annotation.equals(DeleteMapping.class) && object instanceof UUID) {
      return prepareAndSendRestAudit(joinPoint, DELETE, (UUID) object, securityContext);
    } else if (annotation.equals(PutMapping.class)) {
      return prepareAndSendRestAudit(joinPoint, UPSERT, null, securityContext);
    } else {
      throw new AuditException("Cannot save audit for this HTTP method. Not supported annotation: @"
          + annotation.getSimpleName());
    }
  }

  @Around("controller() && args(id, dto, context, securityContext)")
  Object auditPatchPutUpdate(ProceedingJoinPoint joinPoint, UUID id, Object dto, RequestContext context,
      SecurityContext securityContext) throws Throwable {

    var annotation = getAnnotation(joinPoint);

    if (annotation.equals(PutMapping.class) || annotation.equals(PatchMapping.class)) {
      return prepareAndSendRestAudit(joinPoint, UPDATE, id, securityContext);
    } else {
      throw new AuditException("Cannot save audit for this HTTP method. Not supported annotation: @"
          + annotation.getSimpleName());
    }
  }

  private Class<? extends Annotation> getAnnotation(ProceedingJoinPoint joinPoint) {
    var annotations = Arrays.stream(((MethodSignature) joinPoint.getSignature())
            .getMethod()
            .getAnnotations())
        .map(Annotation::annotationType)
        .collect(Collectors.toCollection(ArrayList::new));

    annotations.retainAll(httpAnnotations);
    if (annotations.size() != 1) {
      throw new AuditException(
          String.format(
              "The request handler must have exactly one mapping annotation, but has %d: %s", 
              annotations.size(), annotations));
    }
    return annotations.get(0);
  }

  private void prepareAndSendExceptionAudit(ResponseEntity<?> response,
      AuditableException auditableException) {
    var exceptionAuditEvent = new ExceptionAuditEvent();
    String action;
    if (auditableException.action().isBlank()) {
      if (response.getBody() instanceof DetailedErrorResponse) {
        action = ((DetailedErrorResponse) response.getBody()).getCode();
      } else {
        action = response.getStatusCode().getReasonPhrase();
      }
    } else {
      action = auditableException.action();
    }
    exceptionAuditEvent.setAction(action);

    EventType eventType;
    if (httpStatusOfSecurityAudit.contains(response.getStatusCodeValue()) ||
        responseCodeOfSecurityAudit.contains(action)) {
      eventType = EventType.SECURITY_EVENT;
    } else {
      eventType = EventType.USER_ACTION;
    }
    exceptionAuditEvent.setEventType(eventType);
    exceptionAuditEvent.setUserInfoEnabled(auditableException.userInfoEnabled());

    restAuditEventsFacade.sendExceptionAudit(exceptionAuditEvent);
  }

  private Object prepareAndSendRestAudit(ProceedingJoinPoint joinPoint, String action, UUID id,
      SecurityContext securityContext) throws Throwable {

    String methodName = joinPoint.getSignature().getName();
    String jwt = securityContext == null ? null : securityContext.getAccessToken();

    restAuditEventsFacade
        .sendRestAudit(EventType.USER_ACTION, methodName, action, jwt, BEFORE, id, null);

    Object result = joinPoint.proceed();

    var resultStatus = ((ResponseEntity<?>) result).getStatusCode().getReasonPhrase();

    restAuditEventsFacade.sendRestAudit(EventType.USER_ACTION, methodName,
        action, jwt, AFTER, id, resultStatus);

    return result;
  }
}
