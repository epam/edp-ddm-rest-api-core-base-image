/*
 * Copyright 2023 EPAM Systems.
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

import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import com.epam.digital.data.platform.restapi.core.queryhandler.AbstractQueryHandler;
import com.epam.digital.data.platform.restapi.core.tabledata.TableDataProvider;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;
import org.springframework.boot.test.context.TestComponent;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@TestComponent("queryHandlerFileTest")
public class QueryHandlerFileTestImpl extends AbstractQueryHandler<UUID, MockEntityFile> {

  public QueryHandlerFileTestImpl(TableDataProvider fileTableDataProvider) {
    super(fileTableDataProvider);
  }

  @Override
  public Class<MockEntityFile> entityType() {
    return MockEntityFile.class;
  }

  @Override
  public List<SelectFieldOrAsterisk> selectFields() {
    return Arrays.asList(
        DSL.field("scan_copy"));
  }

  @TestComponent("fileTableDataProvider")
  public static class FileTableDataProviderTestImpl implements TableDataProvider {

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
