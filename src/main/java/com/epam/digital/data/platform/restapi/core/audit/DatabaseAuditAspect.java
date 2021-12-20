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

  @Around("@annotation(com.epam.digital.data.platform.restapi.core.audit.AuditableDatabaseOperation)")
  Object databaseAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
    var signature = (MethodSignature) joinPoint.getSignature();
    var annotation = signature.getMethod().getAnnotation(AuditableDatabaseOperation.class);
    var operation = annotation.value();
    return databaseAuditProcessor.process(joinPoint, operation);
  }
}
