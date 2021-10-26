package com.epam.digital.data.platform.restapi.core.queryhandler.impl;

import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.queryhandler.AbstractQueryHandler;
import com.epam.digital.data.platform.restapi.core.service.AccessPermissionService;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class QueryHandlerTestImpl extends AbstractQueryHandler<UUID, MockEntity> {
  public QueryHandlerTestImpl(
      AccessPermissionService<MockEntity> accessPermissionService) {
    super(accessPermissionService);
  }

  @Override
  public String idName() {
    return "id";
  }

  @Override
  public String tableName() {
    return "table";
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
}
