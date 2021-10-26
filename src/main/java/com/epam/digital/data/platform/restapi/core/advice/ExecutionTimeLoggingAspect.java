package com.epam.digital.data.platform.restapi.core.advice;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

@Aspect
@Component
public class ExecutionTimeLoggingAspect {

  private static final String CEPH_COMMUNICATION_OPERATION_NAME = "Ceph communication";
  private static final String DIGITAL_SIGNATURE_OPS_COMMUNICATION_OPERATION_NAME =
      "Digital signature ops communication";
  private static final String DATABASE_COMMUNICATION_OPERATION_NAME = "Database communication";

  private final Logger log = LoggerFactory.getLogger(ExecutionTimeLoggingAspect.class);

  @Around("within(com.epam.digital.data.platform.integration.ceph.service.CephService+)")
  public Object logCephCommunicationTime(ProceedingJoinPoint joinPoint) throws Throwable {
    return logJoinPointTime(joinPoint, CEPH_COMMUNICATION_OPERATION_NAME);
  }

  @Around("within(com.epam.digital.data.platform.dso.client.*+)")
  public Object logDsoCommunicationTime(ProceedingJoinPoint joinPoint) throws Throwable {
    return logJoinPointTime(joinPoint, DIGITAL_SIGNATURE_OPS_COMMUNICATION_OPERATION_NAME);
  }

  @Around("@annotation(com.epam.digital.data.platform.restapi.core.annotation.DatabaseOperation)")
  public Object logDbCommunicationTime(ProceedingJoinPoint joinPoint) throws Throwable {
    return logJoinPointTime(joinPoint, DATABASE_COMMUNICATION_OPERATION_NAME);
  }

  private Object logJoinPointTime(ProceedingJoinPoint joinPoint, String operationName) throws Throwable {
    var stopwatch = new StopWatch();
    stopwatch.start();
    try {
      return joinPoint.proceed();
    } finally {
      stopwatch.stop();
      log.info("{} took {} ms", operationName, stopwatch.getTotalTimeMillis());
    }
  }
}
