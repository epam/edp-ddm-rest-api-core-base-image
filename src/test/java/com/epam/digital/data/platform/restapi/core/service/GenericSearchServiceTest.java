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

package com.epam.digital.data.platform.restapi.core.service;

import static com.epam.digital.data.platform.model.core.kafka.Status.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.model.core.search.SearchConditionPage;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.service.impl.GenericSearchServiceTestImpl;
import com.epam.digital.data.platform.restapi.core.searchhandler.AbstractSearchHandler;
import com.epam.digital.data.platform.restapi.core.util.MockEntityContains;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = GenericSearchServiceTestImpl.class)
class GenericSearchServiceTest {

  @MockBean
  AbstractSearchHandler<MockEntityContains, MockEntity> searchHandler;

  @Autowired
  GenericSearchServiceTestImpl instance;

  @Test
  void shouldSearchInHandlerWithResult() {
    var c = mockResult();
    var scResponse = new SearchConditionPage<MockEntity>();
    scResponse.setContent(List.of(c));
    given(searchHandler.search(any(Request.class))).willReturn(scResponse);

    var response = instance.request(mockRequest());

    assertThat(response.getStatus()).isEqualTo(SUCCESS);
    assertThat(response.getPayload()).hasSize(1);
    assertThat(response.getPayload().get(0)).isEqualTo(c);
  }

  @Test
  void shouldSearchInHandlerWithEmptyResult() {
    var scResponse = new SearchConditionPage<MockEntity>();
    scResponse.setContent(Collections.emptyList());
    given(searchHandler.search(any(Request.class))).willReturn(scResponse);

    var response = instance.request(mockRequest());

    assertThat(response.getStatus()).isEqualTo(SUCCESS);
    assertThat(response.getPayload()).hasSize(0);
  }

  private MockEntityContains mockSc() {
    return new MockEntityContains();
  }

  private MockEntity mockResult() {
    var c = new MockEntity();
    c.setPersonFullName("Some Full Name");
    return c;
  }

  private Request<MockEntityContains> mockRequest() {
    return new Request<>(mockSc(), new RequestContext(), new SecurityContext());
  }
}
