package com.epam.digital.data.platform.restapi.core.impl.queryhandler;

import com.epam.digital.data.platform.restapi.core.impl.model.TestEntity;
import com.epam.digital.data.platform.restapi.core.queryhandler.AbstractQueryHandler;
import com.epam.digital.data.platform.restapi.core.service.AccessPermissionService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;

public class TestEntityQueryHandler extends AbstractQueryHandler<UUID, TestEntity> {

  public TestEntityQueryHandler(
      AccessPermissionService<TestEntity> accessPermissionService) {
    super(accessPermissionService);
  }

  @Override
  public String idName() {
    return "id";
  }

  @Override
  public String tableName() {
    return "test_entity";
  }

  @Override
  public Class<TestEntity> entityType() {
    return TestEntity.class;
  }

  @Override
  public List<SelectFieldOrAsterisk> selectFields() {
    return Arrays.asList(
        DSL.field("id"),
        DSL.field("consent_date"),
        DSL.field("person_pass_number"),
        DSL.field("person_full_name"),
        DSL.field("person_gender")
    );
  }
}
