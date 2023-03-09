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


import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityContains;
import java.util.Collections;
import java.util.List;
import org.jooq.Condition;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;

public class AbstractSearchHandlerTestImpl extends
    AbstractSearchHandler<MockEntityContains, MockEntity> {

  private String tableName = "table_name";

  @Override
  protected Condition whereClause(MockEntityContains searchCriteria) {
    return DSL.noCondition();
  }

  @Override
  public String tableName() {
    return tableName;
  }

  @Override
  public Class<MockEntity> entityType() {
    return MockEntity.class;
  }

  @Override
  public List<SelectFieldOrAsterisk> selectFields() {
    return Collections.singletonList(DSL.field("consent_id"));
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }
}