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

package com.epam.digital.data.platform.restapi.core.searchhandler;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.audit.AuditableDatabaseOperation;
import com.epam.digital.data.platform.restapi.core.audit.AuditableDatabaseOperation.Operation;
import com.epam.digital.data.platform.restapi.core.exception.SqlErrorException;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class AbstractSearchHandler<I, O> implements SearchHandler<I, O> {

  @Autowired
  protected DSLContext context;

  @AuditableDatabaseOperation(Operation.SEARCH)
  @Override
  public List<O> search(Request<I> input) {
    I searchCriteria = input.getPayload();

    try {
      return
          context
              .select(selectFields())
              .from(DSL.table(tableName()))
              .where(whereClause(searchCriteria))
              .limit(offset(searchCriteria), limit(searchCriteria))
              .fetchInto(entityType());
    } catch (Exception e) {
      throw new SqlErrorException("Can not read from DB", e);
    }
  }

  protected abstract Condition whereClause(I searchCriteria);

  public abstract String tableName();

  public abstract Class<O> entityType();

  public abstract List<SelectFieldOrAsterisk> selectFields();

  public Integer limit(I searchCriteria) {
    return null;
  }

  public Integer offset(I searchCriteria) {
    return null;
  }
}
