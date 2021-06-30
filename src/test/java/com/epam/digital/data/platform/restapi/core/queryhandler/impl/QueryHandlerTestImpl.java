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

package com.epam.digital.data.platform.restapi.core.queryhandler.impl;

import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.model.FieldsAccessCheckDto;
import com.epam.digital.data.platform.restapi.core.queryhandler.AbstractQueryHandler;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.epam.digital.data.platform.restapi.core.tabledata.TableDataProvider;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class QueryHandlerTestImpl extends AbstractQueryHandler<UUID, MockEntity> {
  public QueryHandlerTestImpl(TableDataProvider tableDataProvider) {
    super(tableDataProvider);
  }

  @Override
  public List<FieldsAccessCheckDto> getFieldsToCheckAccess() {
    return List.of(
        new FieldsAccessCheckDto(
            "table", List.of("consent_id", "person_full_name", "person_pass_number")));
  }

  @Override
  public Class<MockEntity> entityType() {
    return MockEntity.class;
  }

  @Override
  public List<SelectFieldOrAsterisk> selectFields() {
    return Arrays.asList(
        DSL.field("consent_id"),
        DSL.field("person_full_name"),
        DSL.field("person_pass_number"));
  }

  @TestComponent
  public static class TableDataProviderTestImpl implements TableDataProvider {

    @Override
    public String tableName() {
      return "table";
    }

    @Override
    public String pkColumnName() {
      return "id";
    }
  }
}
