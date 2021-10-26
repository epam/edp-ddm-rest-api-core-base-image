package com.epam.digital.data.platform.restapi.core.impl.queryhandler;

import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityM2M;
import com.epam.digital.data.platform.restapi.core.queryhandler.AbstractQueryHandler;
import com.epam.digital.data.platform.restapi.core.service.AccessPermissionService;
import com.epam.digital.data.platform.restapi.core.utils.JooqDataTypes;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;

public class TestEntityM2MQueryHandler extends AbstractQueryHandler<UUID, TestEntityM2M> {

  public TestEntityM2MQueryHandler(
      AccessPermissionService<TestEntityM2M> accessPermissionService) {
    super(accessPermissionService);
  }

  @Override
  public String idName() {
    return "id";
  }

  @Override
  public String tableName() {
    return "test_entity_m2m";
  }

  @Override
  public Class<TestEntityM2M> entityType() {
    return TestEntityM2M.class;
  }

  @Override
  public List<SelectFieldOrAsterisk> selectFields() {
    return Arrays.asList(
        DSL.field("id"),
        DSL.field("name"),
        DSL.field("entities", JooqDataTypes.ARRAY_DATA_TYPE)
        );
  }
}
