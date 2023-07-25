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

package com.epam.digital.data.platform.restapi.core.service.impl;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import com.epam.digital.data.platform.restapi.core.queryhandler.AbstractQueryHandler;
import com.epam.digital.data.platform.restapi.core.service.GenericFileQueryService;
import org.springframework.boot.test.context.TestComponent;

import java.util.Optional;
import java.util.UUID;

@TestComponent
public class GenericFileQueryServiceTestImpl
    extends GenericFileQueryService<UUID, MockEntityFile> {

  public GenericFileQueryServiceTestImpl(
          AbstractQueryHandler<UUID, MockEntityFile> queryHandlerFileTest) {
    super(queryHandlerFileTest);
  }

  @Override
  public Optional<File> getFileFieldById(MockEntityFile entity, String requestedFileId) {
    return Optional.of(entity.getScanCopy());
  }
}
