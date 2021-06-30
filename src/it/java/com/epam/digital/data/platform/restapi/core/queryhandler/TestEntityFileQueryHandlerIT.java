package com.epam.digital.data.platform.restapi.core.queryhandler;

import static com.epam.digital.data.platform.restapi.core.util.DaoTestUtils.TEST_ENTITY_FILE;
import static com.epam.digital.data.platform.restapi.core.util.SecurityUtils.mockSecurityContext;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.config.GenericConfig;
import com.epam.digital.data.platform.restapi.core.config.TestConfiguration;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityFile;
import com.epam.digital.data.platform.restapi.core.impl.queryhandler.TestEntityFileQueryHandler;
import com.epam.digital.data.platform.restapi.core.impl.tabledata.TestEntityFileTableDataProvider;
import com.epam.digital.data.platform.restapi.core.service.AccessPermissionService;
import com.epam.digital.data.platform.restapi.core.service.JwtInfoProvider;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.nimbusds.jose.JOSEException;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@TestConfiguration
@SpringBootTest(
    classes = {
      TestEntityFileQueryHandler.class,
      TestEntityFileTableDataProvider.class,
      AccessPermissionService.class,
      JwtInfoProvider.class,
      TokenParser.class,
      GenericConfig.class
    })
class TestEntityFileQueryHandlerIT {

  @Autowired
  TestEntityFileQueryHandler queryHandler;

  TestEntityFile entityFile = TEST_ENTITY_FILE;

  @Test
  @DisplayName("Find by ID")
  void findById() throws JOSEException {
    Optional<TestEntityFile> found =
        queryHandler.findById(
            new Request<>(
                entityFile.getId(),
                null,
                mockSecurityContext()));
    Assertions.assertThat(found).isPresent();
    Assertions.assertThat(found.get().getLegalEntityName())
        .isEqualTo(entityFile.getLegalEntityName());
    Assertions.assertThat(found.get().getScanCopy().getId())
        .isEqualTo(entityFile.getScanCopy().getId());
    Assertions.assertThat(found.get().getScanCopy().getChecksum())
        .isEqualTo(entityFile.getScanCopy().getChecksum());
  }
}
