package com.epam.digital.data.platform.restapi.core.queryhandler;

import static com.epam.digital.data.platform.restapi.core.util.DaoTestUtils.TEST_ENTITY_M2M;
import static com.epam.digital.data.platform.restapi.core.util.SecurityUtils.mockSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.config.GenericConfig;
import com.epam.digital.data.platform.restapi.core.config.TestConfiguration;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityM2M;
import com.epam.digital.data.platform.restapi.core.impl.queryhandler.TestEntityM2MQueryHandler;
import com.epam.digital.data.platform.restapi.core.impl.tabledata.TestEntityM2mTableDataProvider;
import com.epam.digital.data.platform.restapi.core.service.AccessPermissionService;
import com.epam.digital.data.platform.restapi.core.service.JwtInfoProvider;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.nimbusds.jose.JOSEException;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@TestConfiguration
@SpringBootTest(
    classes = {
      TestEntityM2MQueryHandler.class,
      TestEntityM2mTableDataProvider.class,
      AccessPermissionService.class,
      JwtInfoProvider.class,
      TokenParser.class,
      GenericConfig.class
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
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo(entity.getName());
    assertThat(found.get().getEntities()).hasSize(entity.getEntities().length);
    assertThat(found.get().getEntities()[0].getId()).isEqualTo(entity.getEntities()[0].getId());
    assertThat(found.get().getEntities()[0].getPersonFullName())
        .isEqualTo(entity.getEntities()[0].getPersonFullName());
    assertThat(found.get().getEntities()[0].getPersonPassNumber())
        .isEqualTo(entity.getEntities()[0].getPersonPassNumber());
    assertThat(found.get().getEntities()[0].getPersonGender())
        .isEqualTo(entity.getEntities()[0].getPersonGender());
  }
}
