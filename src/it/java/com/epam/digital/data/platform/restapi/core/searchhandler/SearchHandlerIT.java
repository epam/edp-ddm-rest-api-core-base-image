package com.epam.digital.data.platform.restapi.core.searchhandler;

import static com.epam.digital.data.platform.restapi.core.util.DaoTestUtils.TEST_ENTITY;
import static com.epam.digital.data.platform.restapi.core.util.DaoTestUtils.TEST_SINGLE_FIELD_ENTITY;
import static com.epam.digital.data.platform.restapi.core.util.SearchHandlerTestUtil.mockRequest;
import static org.assertj.core.api.Assertions.assertThat;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.search.SearchConditionPage;
import com.epam.digital.data.platform.restapi.core.config.TestConfiguration;
import com.epam.digital.data.platform.restapi.core.impl.model.PagingTestEntitySearchConditions;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntity;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntitySearchConditions;
import com.epam.digital.data.platform.restapi.core.impl.model.TestSingleFieldEntity;
import com.epam.digital.data.platform.restapi.core.impl.model.TestSingleFieldEntitySearchConditions;
import com.epam.digital.data.platform.restapi.core.impl.model.TypGender;
import com.epam.digital.data.platform.restapi.core.impl.searchhandler.PagingTestEntitySearchHandler;
import com.epam.digital.data.platform.restapi.core.impl.searchhandler.TestEntitySearchHandler;

import com.epam.digital.data.platform.restapi.core.impl.searchhandler.TestSingleFieldEntitySearchHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@TestConfiguration
@SpringBootTest(
    classes = {
      TestEntitySearchHandler.class,
      TestSingleFieldEntitySearchHandler.class,
      PagingTestEntitySearchHandler.class
    })
class SearchHandlerIT {

  static final String STARTS_WITH = "John";

  @Autowired
  TestEntitySearchHandler instance;
  @Autowired
  TestSingleFieldEntitySearchHandler singleFieldEntitySearchHandlerInstance;
  @Autowired
  PagingTestEntitySearchHandler pagingInstance;

  TestEntitySearchConditions searchCriteria;
  TestSingleFieldEntitySearchConditions searchSingleFieldCriteria;
  PagingTestEntitySearchConditions pagingSearchCriteria;
  Request<TestEntitySearchConditions> request;
  Request<TestSingleFieldEntitySearchConditions> singleFieldRequest;
  Request<PagingTestEntitySearchConditions> pagingRequest;

  @BeforeEach
  void setup() {
    searchCriteria = new TestEntitySearchConditions();
    request = mockRequest(searchCriteria);
    searchSingleFieldCriteria = new TestSingleFieldEntitySearchConditions();
    singleFieldRequest = mockRequest(searchSingleFieldCriteria);
    pagingSearchCriteria = new PagingTestEntitySearchConditions();
    pagingRequest = mockRequest(pagingSearchCriteria);
  }

  @Test
  void shouldFindAllWhenEmptySearchCriteria() {
    final SearchConditionPage<TestEntity> allRecords = instance.search(request);
    assertThat(allRecords.getContent()).hasSize(3);
  }

  @Test
  void shouldSearchByMultipleSearchCriteria() {
    searchCriteria.setPersonFullName(STARTS_WITH);
    searchCriteria.setPersonGender(TypGender.M);

    final SearchConditionPage<TestEntity> found = instance.search(request);

    assertThat(found.getContent()).hasSize(2);
    assertThat(found.getContent().get(0).getPersonFullName()).isEqualTo(TEST_ENTITY.getPersonFullName());
    assertThat(found.getContent().get(0).getPersonGender()).isEqualTo(TEST_ENTITY.getPersonGender());
    assertThat(found.getTotalElements()).isNull();
    assertThat(found.getTotalPages()).isNull();
    assertThat(found.getPageNo()).isNull();
    assertThat(found.getPageSize()).isNull();
  }
  @Test
  void shouldSearchBySingleSearchCriteria() {
    searchSingleFieldCriteria.setPersonFullName(STARTS_WITH);

    final SearchConditionPage<TestSingleFieldEntity> found = singleFieldEntitySearchHandlerInstance.search(singleFieldRequest);

    assertThat(found.getContent()).hasSize(2);
    assertThat(found.getContent().get(0).getPersonFullName()).isEqualTo(TEST_SINGLE_FIELD_ENTITY.getPersonFullName());
  }

  @Test
  void shouldFindPagedResponseByMultipleSearchCriteria() {
    pagingSearchCriteria.setPersonFullName(STARTS_WITH);
    pagingSearchCriteria.setPersonGender(TypGender.M);
    pagingSearchCriteria.setPageNo(1);
    pagingSearchCriteria.setPageSize(1);

    final SearchConditionPage<TestEntity> found = pagingInstance.search(pagingRequest);

    assertThat(found.getContent()).hasSize(1);
    assertThat(found.getContent().get(0).getPersonFullName()).isEqualTo(TEST_ENTITY.getPersonFullName());
    assertThat(found.getContent().get(0).getPersonGender()).isEqualTo(TEST_ENTITY.getPersonGender());
    assertThat(found.getPageNo()).isEqualTo(1);
    assertThat(found.getPageSize()).isEqualTo(1);
    assertThat(found.getTotalPages()).isEqualTo(2);
    assertThat(found.getTotalElements()).isEqualTo(2);
  }
}
