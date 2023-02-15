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

package com.epam.digital.data.platform.restapi.core.impl.searchhandler;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.search.SearchConditionPage;
import com.epam.digital.data.platform.restapi.core.impl.model.PagingTestEntitySearchConditions;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntity;
import com.epam.digital.data.platform.restapi.core.searchhandler.AbstractSearchHandler;
import com.epam.digital.data.platform.restapi.core.utils.PageableUtils;
import org.jooq.Condition;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class PagingTestEntitySearchHandler
    extends AbstractSearchHandler<PagingTestEntitySearchConditions, TestEntity> {

  @Override
  public SearchConditionPage<TestEntity> search(Request<PagingTestEntitySearchConditions> input) {
    var searchCriteria = input.getPayload();
    SearchConditionPage<TestEntity> response = super.search(input);
    response.setTotalElements(count(input));
    response.setPageSize(limit(searchCriteria));
    response.setTotalPages(
        PageableUtils.getTotalPages(response.getPageSize(), response.getTotalElements()));
    response.setPageNo(
        Optional.ofNullable(searchCriteria.getPageNo()).orElse(PageableUtils.DEFAULT_PAGE_NUMBER));
    return response;
  }

  @Override
  protected Condition whereClause(PagingTestEntitySearchConditions searchConditions) {
    var c = DSL.noCondition();

    if (searchConditions.getPersonGender() != null) {
      c = c.and(DSL.field("person_gender").eq(searchConditions.getPersonGender()).toString());
    }
    if (searchConditions.getPersonFullName() != null) {
      c =
          c.and(
              DSL.field("person_full_name")
                  .startsWithIgnoreCase(searchConditions.getPersonFullName()));
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
    return Arrays.asList(
        DSL.field("id"), DSL.field("person_gender"), DSL.field("person_full_name"));
  }

  @Override
  public Integer limit(PagingTestEntitySearchConditions searchConditions) {
    return Optional.ofNullable(searchConditions.getPageSize())
        .orElse(PageableUtils.DEFAULT_PAGE_SIZE);
  }

  @Override
  public Integer offset(PagingTestEntitySearchConditions searchConditions) {
    return limit(searchConditions)
        * Optional.ofNullable(searchConditions.getPageNo())
            .orElse(PageableUtils.DEFAULT_PAGE_NUMBER);
  }
}
