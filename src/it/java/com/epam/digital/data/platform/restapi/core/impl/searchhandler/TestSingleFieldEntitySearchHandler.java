package com.epam.digital.data.platform.restapi.core.impl.searchhandler;

import com.epam.digital.data.platform.restapi.core.impl.model.TestSingleFieldEntity;
import com.epam.digital.data.platform.restapi.core.impl.model.TestSingleFieldEntitySearchConditions;
import com.epam.digital.data.platform.restapi.core.searchhandler.AbstractSearchHandler;
import org.jooq.Condition;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;

import java.util.Collections;
import java.util.List;

public class TestSingleFieldEntitySearchHandler extends AbstractSearchHandler<
    TestSingleFieldEntitySearchConditions,
    TestSingleFieldEntity> {

  private static final Integer MAX_LIMIT = 10;

  @Override
  protected Condition whereClause(
      TestSingleFieldEntitySearchConditions searchConditions) {
    var c = DSL.noCondition();

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
  public Class<TestSingleFieldEntity> entityType() {
    return TestSingleFieldEntity.class;
  }

  @Override
  public List<SelectFieldOrAsterisk> selectFields() {
    return Collections.singletonList(DSL.field("person_full_name"));
  }

  @Override
  public Integer limit(
      TestSingleFieldEntitySearchConditions searchConditions) {
    if (searchConditions.getLimit() != null) {
      return Math.min(searchConditions.getLimit(), MAX_LIMIT);
    }

    return MAX_LIMIT;
  }

  @Override
  public Integer offset(
      TestSingleFieldEntitySearchConditions searchConditions) {
    return searchConditions.getOffset();
  }
}
