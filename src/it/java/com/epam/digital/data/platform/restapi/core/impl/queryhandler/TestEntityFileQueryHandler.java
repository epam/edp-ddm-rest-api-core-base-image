package com.epam.digital.data.platform.restapi.core.impl.queryhandler;

import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityFile;
import com.epam.digital.data.platform.restapi.core.impl.tabledata.TestEntityFileTableDataProvider;
import com.epam.digital.data.platform.restapi.core.queryhandler.AbstractQueryHandler;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import com.epam.digital.data.platform.restapi.core.utils.JooqDataTypes;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;

public class TestEntityFileQueryHandler extends AbstractQueryHandler<UUID, TestEntityFile> {

  public TestEntityFileQueryHandler(
          TestEntityFileTableDataProvider tableDataProvider) {
    super(tableDataProvider);
  }

  @Override
  public Class<TestEntityFile> entityType() {
    return TestEntityFile.class;
  }

  @Override
  public List<SelectFieldOrAsterisk> selectFields() {
    return Arrays.asList(
        DSL.field("id"),
        DSL.field("legal_entity_name"),
        DSL.field("scan_copy", JooqDataTypes.FILE_DATA_TYPE
        ));
  }
}
