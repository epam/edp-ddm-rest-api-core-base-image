package com.epam.digital.data.platform.restapi.core.searchhandler;

import static com.epam.digital.data.platform.restapi.core.util.DaoTestUtils.TEST_ENTITY_FILE;
import static com.epam.digital.data.platform.restapi.core.util.SearchHandlerTestUtil.mockRequest;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.config.TestConfiguration;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityFile;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityFileSearchConditions;
import com.epam.digital.data.platform.restapi.core.impl.searchhandler.TestEntityFileSearchHandler;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@TestConfiguration
@SpringBootTest(classes = {TestEntityFileSearchHandler.class})
class TestEntityFileSearchHandlerIT {

  static final String STARTS_WITH = "FOP John";

  @Autowired
  TestEntityFileSearchHandler instance;

  TestEntityFileSearchConditions searchCriteria;
  Request<TestEntityFileSearchConditions> request;

  @BeforeEach
  void setup() {
    searchCriteria = new TestEntityFileSearchConditions();
    request = mockRequest(searchCriteria);
  }

  @Test
  void shouldFindAllWhenEmptySearchCriteria() {
    final List<TestEntityFile> allRecords = instance.search(request);
    Assertions.assertThat(allRecords).hasSize(2);
  }

  @Test
  void shouldSearchByMultipleSearchCriteria() {
    searchCriteria.setLegalEntityName(STARTS_WITH);

    final List<TestEntityFile> found = instance.search(request);

    Assertions.assertThat(found).hasSize(1);
    Assertions.assertThat(found.get(0).getLegalEntityName()).isEqualTo(TEST_ENTITY_FILE.getLegalEntityName());
    Assertions.assertThat(found.get(0).getScanCopy().getId())
        .isEqualTo(TEST_ENTITY_FILE.getScanCopy().getId());
    Assertions.assertThat(found.get(0).getScanCopy().getChecksum())
        .isEqualTo(TEST_ENTITY_FILE.getScanCopy().getChecksum());
  }
}
