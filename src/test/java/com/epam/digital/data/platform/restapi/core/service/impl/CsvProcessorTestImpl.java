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

package com.epam.digital.data.platform.restapi.core.service.impl;

import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityCreateList;
import com.epam.digital.data.platform.restapi.core.service.AbstractCsvProcessor;
import org.springframework.boot.test.context.TestComponent;

import java.util.List;

@TestComponent
public class CsvProcessorTestImpl
    extends AbstractCsvProcessor<MockEntity, MockEntityCreateList> {

  @Override
  protected Class<MockEntity> getCsvRowElementType() {
    return MockEntity.class;
  }

  @Override
  protected MockEntityCreateList getPayloadObjectFromCsvRows(List<MockEntity> rows) {
    var payload = new MockEntityCreateList();
    payload.setEntities(rows.toArray(new MockEntity[0]));
    return payload;
  }
}
