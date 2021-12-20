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

  @Around("@annotation(com.epam.digital.data.platform.restapi.core.audit.AuditableDatabaseOperation)")
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
