package com.epam.digital.data.platform.restapi.core.impl.queryhandler;

import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityM2M;
import com.epam.digital.data.platform.restapi.core.impl.tabledata.TestEntityM2mTableDataProvider;
import com.epam.digital.data.platform.restapi.core.model.FieldsAccessCheckDto;
import com.epam.digital.data.platform.restapi.core.queryhandler.AbstractQueryHandler;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;

public class TestEntityM2MQueryHandler extends AbstractQueryHandler<UUID, TestEntityM2M> {

  public TestEntityM2MQueryHandler(
          TestEntityM2mTableDataProvider tableDataProvider) {
    super(tableDataProvider);
  }

  @Override
  public List<FieldsAccessCheckDto> getFieldsToCheckAccess() {
    return Arrays.asList(
        new FieldsAccessCheckDto("test_entity_m2m", Arrays.asList("id", "name", "entities")),
        new FieldsAccessCheckDto(
            "test_entity",
            Arrays.asList(
                "id", "consent_date", "person_pass_number", "person_full_name", "person_gender")));
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
        DSL.field(
                DSL.select(
                        DSL.coalesce(
                            DSL.jsonArrayAgg(
                                DSL.jsonObject(
                                    DSL.field("id"),
                                    DSL.field("person_pass_number"),
                                    DSL.field("consent_date"),
                                    DSL.field("person_gender"),
                                    DSL.field("person_full_name"))),
                            DSL.jsonArray()))
                    .from("test_entity")
                    .where(
                        DSL.field("id")
                            .eq(
                                DSL.any(
                                    DSL.array(DSL.field("test_entity_m2m.entities"))))))
            .as("entities"));
  }
}
