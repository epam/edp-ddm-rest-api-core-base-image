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
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.queryhandler.QueryHandler;
import java.util.Optional;

public abstract class GenericQueryService<I, O> {

  private final QueryHandler<I, O> queryHandler;

  protected GenericQueryService(QueryHandler<I, O> queryHandler) {
    this.queryHandler = queryHandler;
  }

  public Response<O> request(Request<I> input) {
    Response<O> response = new Response<>();

    Optional<O> consent = queryHandler.findById(input);
    if (consent.isPresent()) {
      response.setPayload(consent.get());
      response.setStatus(Status.SUCCESS);
    } else {
      response.setStatus(Status.NOT_FOUND);
    }

    return response;
  }
}
