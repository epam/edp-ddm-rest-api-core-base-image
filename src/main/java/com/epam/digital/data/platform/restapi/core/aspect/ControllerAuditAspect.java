package com.epam.digital.data.platform.restapi.core.aspect;

import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.model.DetailedErrorResponse;
import com.epam.digital.data.platform.restapi.core.service.RestAuditEventsFacade;
import com.epam.digital.data.platform.restapi.core.utils.ResponseCode;
import com.epam.digital.data.platform.starter.audit.model.EventType;
import java.util.Set;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
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

  @Pointcut("execution(* com.epam.digital.data.platform.restapi.core.exception.ApplicationExceptionHandler.*(..))")
  public void exceptionHandlerPointcut() {
  }

  @Pointcut("execution(public * com.epam.digital.data.platform.starter.security.jwt.TokenParser.parseClaims(..))")
  public void jwtParsingPointcut() {
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

  @AfterReturning(pointcut = "exceptionHandlerPointcut()", returning = "response")
  void exceptionAudit(ResponseEntity<?> response) {
    prepareAndSendExceptionAudit(response);
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

  @AfterThrowing("jwtParsingPointcut()")
  void auditInvalidJwt() {
    restAuditEventsFacade.auditInvalidAccessToken();
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

  private void prepareAndSendExceptionAudit(ResponseEntity<?> response) {
    var eventType = EventType.USER_ACTION;

    String action = response.getStatusCode().getReasonPhrase();
    if (response.getBody() instanceof DetailedErrorResponse) {
      action = ((DetailedErrorResponse) response.getBody()).getCode();
    }

    if (httpStatusOfSecurityAudit.contains(response.getStatusCodeValue()) ||
        responseCodeOfSecurityAudit.contains(action)) {
      eventType = EventType.SECURITY_EVENT;
    }
    restAuditEventsFacade.sendExceptionAudit(eventType, action);
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
