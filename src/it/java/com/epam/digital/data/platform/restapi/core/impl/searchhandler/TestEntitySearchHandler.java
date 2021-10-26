package com.epam.digital.data.platform.restapi.core.impl.searchhandler;

import com.epam.digital.data.platform.restapi.core.impl.model.TestEntity;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntitySearchConditions;
import com.epam.digital.data.platform.restapi.core.searchhandler.AbstractSearchHandler;
import java.util.Arrays;
import java.util.List;
import org.jooq.Condition;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;

public class TestEntitySearchHandler extends AbstractSearchHandler<
    TestEntitySearchConditions,
    TestEntity> {

  private static final Integer MAX_LIMIT = 10;

  @Override
  protected Condition whereClause(
      TestEntitySearchConditions searchConditions) {
    var c = DSL.noCondition();

    if (searchConditions.getPersonGender() != null) {
      c = c.and(DSL.field("person_gender").eq(searchConditions.getPersonGender()).toString());
    }
    if (searchConditions.getPersonFullName() != null) {
      c = c.and(DSL.field("person_full_name").startsWithIgnoreCase(searchConditions.getPersonFullName()));
    }

    return c;
  }

  @Override
  public String tableName() {
    return "test_entity_by_enum_and_name_starts_with_limit_offset_v";
  }

  @Override
  public Class<TestEntity> entityType() {
    return TestEntity.class;
  }

  @Override
  public List<SelectFieldOrAsterisk> selectFields() {
    return Arrays.asList(DSL.field("id"),
        DSL.field("person_gender"),
        DSL.field("person_full_name"));
  }

  @Override
  public Integer limit(
      TestEntitySearchConditions searchConditions) {
    if (searchConditions.getLimit() != null) {
      return Math.min(searchConditions.getLimit(), MAX_LIMIT);
    }

    return MAX_LIMIT;
  }

  @Override
  public Integer offset(
      TestEntitySearchConditions searchConditions) {
    return searchConditions.getOffset();
  }
}
