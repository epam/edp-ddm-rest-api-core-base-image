package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.annotation.AuditableException;
import com.epam.digital.data.platform.restapi.core.model.DetailedErrorResponse;
import com.epam.digital.data.platform.restapi.core.model.audit.ExceptionAuditEvent;
import com.epam.digital.data.platform.restapi.core.utils.ResponseCode;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import java.util.Set;
import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class ControllerAuditAspect {

  Set<Integer> httpStatusOfSecurityAudit = Set.of(401, 403, 412);
  private final Set<String> responseCodeOfSecurityAudit = Set.of(
      ResponseCode.AUTHENTICATION_FAILED,
      ResponseCode.SIGNATURE_VIOLATION,
      ResponseCode.JWT_INVALID,
      ResponseCode.JWT_EXPIRED,
      ResponseCode.FORBIDDEN_OPERATION
  );
  // action
  static final String CREATE = "CREATE ENTITY";
  static final String READ = "READ ENTITY";
  static final String UPDATE = "UPDATE ENTITY";
  static final String DELETE = "DELETE ENTITY";
  static final String SEARCH = "SEARCH ENTITY";

  // step
  static final String BEFORE = "BEFORE";
  static final String AFTER = "AFTER";

  private final RestAuditEventsFacade restAuditEventsFacade;

  public ControllerAuditAspect(RestAuditEventsFacade restAuditEventsFacade) {
    this.restAuditEventsFacade = restAuditEventsFacade;
  }

  @Pointcut("@within(org.springframework.web.bind.annotation.RestController)")
  public void withinRestControllerPointcut() {
  }

  @Pointcut("@annotation(com.epam.digital.data.platform.restapi.core.annotation.AuditableException)")
  public void auditableExceptionPointcut() {
  }

  @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping) && withinRestControllerPointcut()")
  public void getPointcut() {
  }

  @Pointcut("@annotation(org.springframework.web.bind.annotation.PostMapping) && withinRestControllerPointcut()")
  public void postPointcut() {
  }

  @Pointcut("@annotation(org.springframework.web.bind.annotation.PutMapping) && withinRestControllerPointcut()")
  public void putPointcut() {
  }

  @Pointcut("@annotation(org.springframework.web.bind.annotation.PatchMapping) && withinRestControllerPointcut()")
  public void patchPointcut() {
  }

  @Pointcut("@annotation(org.springframework.web.bind.annotation.DeleteMapping) && withinRestControllerPointcut()")
  public void deletePointcut() {
  }

  @AfterReturning(pointcut = "auditableExceptionPointcut()", returning = "response")
  void exceptionAudit(JoinPoint joinPoint, ResponseEntity<?> response) {
    var auditableExceptionAnnotation = ((MethodSignature)joinPoint.getSignature()).getMethod()
            .getAnnotation(AuditableException.class);
    prepareAndSendExceptionAudit(response, auditableExceptionAnnotation);
  }

  @Around("postPointcut() && args(dto, context, securityContext)")
  Object auditPost(ProceedingJoinPoint joinPoint, Object dto, RequestContext context,
      SecurityContext securityContext) throws Throwable {
    return prepareAndSendRestAudit(joinPoint, CREATE, null, securityContext);
  }

  @Around("(putPointcut() || patchPointcut()) && args(id, dto, context, securityContext)")
  Object auditUpdate(ProceedingJoinPoint joinPoint, UUID id, Object dto, RequestContext context,
      SecurityContext securityContext) throws Throwable {
    return prepareAndSendRestAudit(joinPoint, UPDATE, id, securityContext);
  }

  @Around("deletePointcut() && args(id, context, securityContext)")
  Object auditDelete(ProceedingJoinPoint joinPoint, UUID id, RequestContext context,
      SecurityContext securityContext) throws Throwable {
    return prepareAndSendRestAudit(joinPoint, DELETE, id, securityContext);
  }

  @Around("getPointcut() && args(dto, context, securityContext)")
  Object auditGetSearch(ProceedingJoinPoint joinPoint, Object dto, RequestContext context,
      SecurityContext securityContext) throws Throwable {

    String action = SEARCH;
    UUID id = null;

    if(dto instanceof UUID) {
      action = READ;
      id = (UUID)dto;
    }

    return prepareAndSendRestAudit(joinPoint, action, id, securityContext);
  }

  private void prepareAndSendExceptionAudit(ResponseEntity<?> response, AuditableException auditableException) {
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
