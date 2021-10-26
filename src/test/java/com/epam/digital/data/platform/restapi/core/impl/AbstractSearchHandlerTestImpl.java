package com.epam.digital.data.platform.restapi.core.impl;

import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.searchhandler.AbstractSearchHandler;
import com.epam.digital.data.platform.restapi.core.util.MockEntityContains;
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
    return Collections.singletonList(DSL.field("field"));
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }
}