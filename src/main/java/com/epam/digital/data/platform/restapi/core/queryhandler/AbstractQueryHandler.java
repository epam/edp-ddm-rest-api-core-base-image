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

package com.epam.digital.data.platform.restapi.core.queryhandler;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.audit.AuditableDatabaseOperation;
import com.epam.digital.data.platform.restapi.core.audit.AuditableDatabaseOperation.Operation;
import com.epam.digital.data.platform.restapi.core.exception.ForbiddenOperationException;
import com.epam.digital.data.platform.restapi.core.exception.SqlErrorException;
import com.epam.digital.data.platform.restapi.core.model.FieldsAccessCheckDto;
import com.epam.digital.data.platform.restapi.core.service.AccessPermissionService;
import com.epam.digital.data.platform.restapi.core.service.JwtInfoProvider;
import com.epam.digital.data.platform.restapi.core.tabledata.TableDataProvider;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

public abstract class AbstractQueryHandler<I, O> implements QueryHandler<I, O> {

  private final Logger log = LoggerFactory.getLogger(AbstractQueryHandler.class);

  @Autowired
  protected DSLContext context;
  @Autowired
  protected JwtInfoProvider jwtInfoProvider;
  @Autowired
  protected AccessPermissionService accessPermissionService;

  protected final TableDataProvider tableDataProvider;

  public AbstractQueryHandler(TableDataProvider tableDataProvider) {
    this.tableDataProvider = tableDataProvider;
  }

  @AuditableDatabaseOperation(Operation.READ)
  @Override
  public Optional<O> findById(Request<I> input) {
    log.info("Reading from DB");

    validateAccess(input);

    I id = input.getPayload();
    try {
      final O dto =
          context
              .select(selectFields())
              .from(DSL.table(tableDataProvider.tableName()))
              .where(DSL.field(tableDataProvider.pkColumnName()).eq(id))
                  .and(getCommonCondition(input))
              .fetchOneInto(entityType());
      return Optional.ofNullable(dto);
    } catch (Exception e) {
      throw new SqlErrorException("Can not read from DB", e);
    }
  }

  public void validateAccess(Request<I> input) {
    JwtClaimsDto userClaims = jwtInfoProvider.getUserClaims(input);
    if (!accessPermissionService.hasReadAccess(getFieldsToCheckAccess(), userClaims)) {
      throw new ForbiddenOperationException(
              "User has invalid role for search by ID from " + tableDataProvider.tableName());
    }
  }

  public Condition getCommonCondition(Request<I> input) {
    return DSL.noCondition();
  }

  public abstract List<FieldsAccessCheckDto> getFieldsToCheckAccess();

  public abstract Class<O> entityType();

  public abstract List<SelectFieldOrAsterisk> selectFields();
}
