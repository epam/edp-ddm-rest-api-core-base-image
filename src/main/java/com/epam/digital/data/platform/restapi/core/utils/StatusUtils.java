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

package com.epam.digital.data.platform.restapi.core.utils;

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import org.springframework.http.HttpStatus;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

public final class StatusUtils {

  private static final Map<Status, HttpStatus> kafkaResponseToHttpStatusMap;

  static {
    kafkaResponseToHttpStatusMap = new EnumMap<>(Status.class);
    kafkaResponseToHttpStatusMap.put(Status.SUCCESS, HttpStatus.OK);
    kafkaResponseToHttpStatusMap.put(Status.CREATED, HttpStatus.CREATED);
    kafkaResponseToHttpStatusMap.put(Status.NO_CONTENT, HttpStatus.NO_CONTENT);
    kafkaResponseToHttpStatusMap.put(Status.NOT_FOUND, HttpStatus.NOT_FOUND);
    kafkaResponseToHttpStatusMap
        .put(Status.THIRD_PARTY_SERVICE_UNAVAILABLE, HttpStatus.INTERNAL_SERVER_ERROR);
    kafkaResponseToHttpStatusMap.put(Status.PROCEDURE_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    kafkaResponseToHttpStatusMap
        .put(Status.INTERNAL_CONTRACT_VIOLATION, HttpStatus.INTERNAL_SERVER_ERROR);
    kafkaResponseToHttpStatusMap.put(Status.INVALID_SIGNATURE, HttpStatus.INTERNAL_SERVER_ERROR);
    kafkaResponseToHttpStatusMap.put(Status.CONSTRAINT_VIOLATION, HttpStatus.CONFLICT);
    kafkaResponseToHttpStatusMap.put(Status.JWT_EXPIRED, HttpStatus.FORBIDDEN);
    kafkaResponseToHttpStatusMap.put(Status.JWT_INVALID, HttpStatus.FORBIDDEN);
    kafkaResponseToHttpStatusMap.put(Status.FORBIDDEN_OPERATION, HttpStatus.FORBIDDEN);
    kafkaResponseToHttpStatusMap.put(Status.SQL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  private StatusUtils() {
  }

  public static HttpStatus convertResponseStatus(Response<?> response) {
    return Optional.ofNullable(kafkaResponseToHttpStatusMap.get(response.getStatus()))
        .orElseThrow(() -> new IllegalArgumentException("Unknown status " + response.getStatus()));
  }
}
