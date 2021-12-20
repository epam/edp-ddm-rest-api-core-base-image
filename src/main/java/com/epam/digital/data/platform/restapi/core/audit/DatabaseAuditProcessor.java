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

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.audit.AuditableDatabaseOperation.Operation;
import com.epam.digital.data.platform.restapi.core.converter.EntityConverter;
import com.epam.digital.data.platform.restapi.core.exception.AuditException;
import com.epam.digital.data.platform.restapi.core.service.JwtInfoProvider;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import java.util.Optional;
import java.util.Set;
import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DatabaseAuditProcessor implements AuditProcessor<Operation> {

  private final Logger log = LoggerFactory.getLogger(DatabaseAuditProcessor.class);

  // action
  static final String READ = "SELECT FROM TABLE";
  static final String SEARCH = "SEARCH";

  // step
  static final String BEFORE = "BEFORE";
  static final String AFTER = "AFTER";

  private final DatabaseEventsFacade databaseEventsFacade;
  private final JwtInfoProvider jwtInfoProvider;
  private final EntityConverter<Object> entityConverter;

  public DatabaseAuditProcessor(
      DatabaseEventsFacade databaseEventsFacade,
      JwtInfoProvider jwtInfoProvider,
      EntityConverter<Object> entityConverter) {
    this.databaseEventsFacade = databaseEventsFacade;
    this.jwtInfoProvider = jwtInfoProvider;
    this.entityConverter = entityConverter;
  }

  @Override
  public Object process(ProceedingJoinPoint joinPoint, Operation operation) throws Throwable {
    switch (operation) {
      case READ:
        return findById(joinPoint);
      case SEARCH:
        return search(joinPoint);
      default:
        throw new AuditException("Unsupported audit operation");
    }
  }

  private Object findById(ProceedingJoinPoint joinPoint) throws Throwable {
    var request = getArgumentByType(joinPoint, Request.class);

    var userClaims = jwtInfoProvider.getUserClaims(request);
    var entityId = request.getPayload().toString();

    return prepareAndSendDbAudit(joinPoint, READ, userClaims, null, entityId);
  }

  private Object search(ProceedingJoinPoint joinPoint) throws Throwable {
    var request = getArgumentByType(joinPoint, Request.class);

    JwtClaimsDto userClaims = jwtInfoProvider.getUserClaims(request);
    Set<String> fields = getFields(request.getPayload());

    return prepareAndSendDbAudit(joinPoint, SEARCH, userClaims, fields, null);
  }

  private Object prepareAndSendDbAudit(
      ProceedingJoinPoint joinPoint, String action, JwtClaimsDto userClaims,
      Set<String> fields, String entityId) throws Throwable {

    String methodName = joinPoint.getSignature().getName();

    log.debug("Sending {} event to Audit", action);
    databaseEventsFacade
        .sendDbAudit(methodName, null, action, userClaims, BEFORE, entityId, fields, null);

    Object result = joinPoint.proceed();

    if (action.equals(READ)) {
      fields = ((Optional<?>) result).map(this::getFields).orElse(null);
    }

    log.debug("Sending {} completed event to Audit", action);
    databaseEventsFacade
        .sendDbAudit(methodName, null, action, userClaims, AFTER, entityId, fields, null);
    return result;
  }


  private Set<String> getFields(Object dto) {
    if (dto == null) {
      return null;
    }

    Set<String> fields = entityConverter.entityToMap(dto).keySet();
    if (fields.isEmpty()) {
      fields = null;
    }

    return fields;
  }
}
