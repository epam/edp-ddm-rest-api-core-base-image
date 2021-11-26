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

package com.epam.digital.data.platform.restapi.core.config;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Record7;
import org.jooq.Result;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockDataProvider;
import org.jooq.tools.jdbc.MockExecuteContext;
import org.jooq.tools.jdbc.MockResult;

public class TestDataProvider implements MockDataProvider {

  public static final UUID ENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
  public static final UUID ENTITY_ID_2 = UUID.fromString("123e4567-e89b-12d3-a456-426655440001");
  public static final UUID ENTITY_ID_3 = UUID.fromString("123e4567-e89b-12d3-a456-426655440002");

  @Override
  public MockResult[] execute(MockExecuteContext ctx) {
    final MockResult[] mockResults = new MockResult[1];
    DSLContext create = DSL.using(SQLDialect.POSTGRES);

    final String sql = ctx.sql();
    final boolean isExecutableStatement =
        sql.startsWith("delete") || sql.startsWith("update") || sql.startsWith("insert");

    if (isExecutableStatement) {
      final Result<Record> record = create.newResult(DSL.table("table"));
      mockResults[0] = new MockResult(1, null);
      return mockResults;
    }

    List<Field<Object>> fields =
        Arrays.asList(
            DSL.field("consent_id"),
            DSL.field("person_full_name"),
            DSL.field("person_pass_number"),
            DSL.field("ddm_created_at"),
            DSL.field("ddm_updated_at"),
            DSL.field("ddm_created_by"),
            DSL.field("ddm_updated_by"));
    Result result = create.newResult(fields);

    mockResults[0] = generateDataForFindOneQuery(create, fields, result);
    return mockResults;
  }

  private MockResult generateDataForFindOneQuery(
      DSLContext create, List<Field<Object>> fields, Result result) {
    final Record7 record = (Record7) create.newRecord(fields);
    record
        .value1(ENTITY_ID)
        .value2("Roman")
        .value3("АА000000")
        .value4(LocalDateTime.now())
        .value5(LocalDateTime.now())
        .value6("Roman")
        .value7("Roman");
    result.add(record);
    return new MockResult(1, result);
  }
}
