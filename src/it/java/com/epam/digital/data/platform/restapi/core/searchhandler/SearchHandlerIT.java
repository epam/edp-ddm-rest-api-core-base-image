package com.epam.digital.data.platform.restapi.core.searchhandler;

import static com.epam.digital.data.platform.restapi.core.util.DaoTestUtils.TEST_ENTITY;
import static com.epam.digital.data.platform.restapi.core.util.DaoTestUtils.TEST_SINGLE_FIELD_ENTITY;
import static com.epam.digital.data.platform.restapi.core.util.SearchHandlerTestUtil.mockRequest;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.config.TestConfiguration;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntity;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntitySearchConditions;
import com.epam.digital.data.platform.restapi.core.impl.model.TestSingleFieldEntity;
import com.epam.digital.data.platform.restapi.core.impl.model.TestSingleFieldEntitySearchConditions;
import com.epam.digital.data.platform.restapi.core.impl.model.TypGender;
import com.epam.digital.data.platform.restapi.core.impl.searchhandler.TestEntitySearchHandler;
import java.util.List;

import com.epam.digital.data.platform.restapi.core.impl.searchhandler.TestSingleFieldEntitySearchHandler;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@TestConfiguration
@SpringBootTest(classes = {TestEntitySearchHandler.class, TestSingleFieldEntitySearchHandler.class})
class SearchHandlerIT {

  static final String STARTS_WITH = "John";

  @Autowired
  TestEntitySearchHandler instance;
  @Autowired
  TestSingleFieldEntitySearchHandler singleFieldEntitySearchHandlerInstance;

  TestEntitySearchConditions searchCriteria;
  TestSingleFieldEntitySearchConditions searchSingleFieldCriteria;
  Request<TestEntitySearchConditions> request;
  Request<TestSingleFieldEntitySearchConditions> singleFieldRequest;

  @BeforeEach
  void setup() {
    searchCriteria = new TestEntitySearchConditions();
    request = mockRequest(searchCriteria);
    searchSingleFieldCriteria = new TestSingleFieldEntitySearchConditions();
    singleFieldRequest = mockRequest(searchSingleFieldCriteria);
  }

  @Test
  void shouldFindAllWhenEmptySearchCriteria() {
    final List<TestEntity> allRecords = instance.search(request);
    Assertions.assertThat(allRecords).hasSize(3);
  }

  @Test
  void shouldSearchByMultipleSearchCriteria() {
    searchCriteria.setPersonFullName(STARTS_WITH);
    searchCriteria.setPersonGender(TypGender.M);

    final List<TestEntity> found = instance.search(request);

    Assertions.assertThat(found).hasSize(2);
    Assertions.assertThat(found.get(0).getPersonFullName()).isEqualTo(TEST_ENTITY.getPersonFullName());
    Assertions.assertThat(found.get(0).getPersonGender()).isEqualTo(TEST_ENTITY.getPersonGender());
  }
  @Test
  void shouldSearchBySingleSearchCriteria() {
    searchSingleFieldCriteria.setPersonFullName(STARTS_WITH);

    final List<TestSingleFieldEntity> found = singleFieldEntitySearchHandlerInstance.search(singleFieldRequest);

    Assertions.assertThat(found).hasSize(2);
    Assertions.assertThat(found.get(0).getPersonFullName()).isEqualTo(TEST_SINGLE_FIELD_ENTITY.getPersonFullName());
  }
}
