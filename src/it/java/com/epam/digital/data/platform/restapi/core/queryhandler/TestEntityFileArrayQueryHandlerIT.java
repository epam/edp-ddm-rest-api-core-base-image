/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.restapi.core.queryhandler;

import static com.epam.digital.data.platform.restapi.core.util.DaoTestUtils.TEST_ENTITY_FILE_ARRAY;
import static com.epam.digital.data.platform.restapi.core.util.SecurityUtils.mockSecurityContext;
import static org.assertj.core.api.Assertions.assertThat;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.config.GenericConfig;
import com.epam.digital.data.platform.restapi.core.config.TestConfiguration;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityFileArray;
import com.epam.digital.data.platform.restapi.core.impl.queryhandler.TestEntityFileArrayQueryHandler;
import com.epam.digital.data.platform.restapi.core.impl.tabledata.TestEntityFileArrayTableDataProvider;
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
      TestEntityFileArrayQueryHandler.class,
      TestEntityFileArrayTableDataProvider.class,
      AccessPermissionService.class,
      JwtInfoProvider.class,
      TokenParser.class,
      GenericConfig.class
    })
class TestEntityFileArrayQueryHandlerIT {

  @Autowired
  TestEntityFileArrayQueryHandler queryHandler;

  TestEntityFileArray entityFile = TEST_ENTITY_FILE_ARRAY;

  @Test
  @DisplayName("Find by ID")
  void findById() throws JOSEException {
    Optional<TestEntityFileArray> found =
        queryHandler.findById(
            new Request<>(
                entityFile.getId(),
                null,
                mockSecurityContext()));
    assertThat(found).isPresent();
    assertThat(found.get().getLegalEntityName()).isEqualTo(entityFile.getLegalEntityName());
    assertThat(found.get().getScanCopies()).hasSize(2);
    
    var file0 = found.get().getScanCopies().get(0);
    assertThat(file0.getId()).isEqualTo(entityFile.getScanCopies().get(0).getId());
    assertThat(file0.getChecksum()).isEqualTo(entityFile.getScanCopies().get(0).getChecksum()); 
    
    var file1 = found.get().getScanCopies().get(1);
    assertThat(file1.getId()).isEqualTo(entityFile.getScanCopies().get(1).getId());
    assertThat(file1.getChecksum()).isEqualTo(entityFile.getScanCopies().get(1).getChecksum());
  }
}
