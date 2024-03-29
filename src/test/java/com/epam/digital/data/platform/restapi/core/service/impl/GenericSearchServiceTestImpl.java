/*
 * Copyright 2021 EPAM Systems.
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

import com.epam.digital.data.platform.model.core.search.SearchConditionPage;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.searchhandler.AbstractSearchHandler;
import com.epam.digital.data.platform.restapi.core.service.GenericSearchService;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityContains;
import org.springframework.boot.test.context.TestComponent;

import java.util.List;

@TestComponent
public class GenericSearchServiceTestImpl
    extends GenericSearchService<MockEntityContains, MockEntity, List<MockEntity>> {

  protected GenericSearchServiceTestImpl(
      AbstractSearchHandler<MockEntityContains, MockEntity> searchHandler) {
    super(searchHandler);
  }

  @Override
  protected List<MockEntity> getResponsePayload(SearchConditionPage<MockEntity> page) {
    return page.getContent();
  }
}
