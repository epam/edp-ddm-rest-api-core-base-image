package com.epam.digital.data.platform.restapi.core.queryhandler;

import static com.epam.digital.data.platform.restapi.core.util.DaoTestUtils.TEST_ENTITY_M2M;
import static com.epam.digital.data.platform.restapi.core.util.SecurityUtils.mockSecurityContext;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.config.TestConfiguration;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityM2M;
import com.epam.digital.data.platform.restapi.core.impl.queryhandler.TestEntityM2MQueryHandler;
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
      TestEntityM2MQueryHandler.class,
      AccessPermissionService.class,
      JwtInfoProvider.class,
      TokenParser.class
    })
class TestEntityM2MQueryHandlerIT {

  @Autowired
  TestEntityM2MQueryHandler queryHandler;

  TestEntityM2M entity = TEST_ENTITY_M2M;

  @Test
  @DisplayName("Find by ID")
  void findById() throws JOSEException {
    Optional<TestEntityM2M> found =
        queryHandler.findById(
            new Request<>(
                entity.getId(),
                null,
                mockSecurityContext()));
    Assertions.assertThat(found).isPresent();
    Assertions.assertThat(found.get().getName()).isEqualTo(entity.getName());
    Assertions.assertThat(found.get().getEntities()).hasSize(entity.getEntities().size());
    Assertions.assertThat(found.get().getEntities().get(0)).isEqualTo(entity.getEntities().get(0));
  }
}
