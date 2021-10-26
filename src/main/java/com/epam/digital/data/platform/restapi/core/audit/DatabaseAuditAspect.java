package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.restapi.core.annotation.DatabaseOperation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class DatabaseAuditAspect {

  private final DatabaseAuditProcessor databaseAuditProcessor;

  public DatabaseAuditAspect(DatabaseAuditProcessor databaseAuditProcessor) {
    this.databaseAuditProcessor = databaseAuditProcessor;
  }

  @Around("@annotation(com.epam.digital.data.platform.restapi.core.annotation.DatabaseOperation)")
  Object databaseAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    var signature = (MethodSignature) joinPoint.getSignature();
    var annotation = signature.getMethod().getAnnotation(DatabaseOperation.class);
    var operation = annotation.value();
    return databaseAuditProcessor.process(joinPoint, operation);
  }
}
