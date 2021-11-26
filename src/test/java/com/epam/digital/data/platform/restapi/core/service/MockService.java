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

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import org.springframework.boot.test.context.TestComponent;

import java.util.UUID;

import static org.mockito.Mockito.mock;

@TestComponent
public class MockService {

  public Response<MockEntity> search(Object searchConditions) {
    return mock(Response.class);
  }

  public Response<MockEntity> read(Request<UUID> request) {
    return mock(Response.class);
  }

  public Response<Void> create(Request<MockEntity> request) {
    return mock(Response.class);
  }

  public Response<Void> update(Request<MockEntity> request) {
    return mock(Response.class);
  }

  public Response<Void> delete(Request<MockEntity> request) {
    return mock(Response.class);
  }
}
