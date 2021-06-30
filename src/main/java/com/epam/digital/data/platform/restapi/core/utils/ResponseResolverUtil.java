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
import com.epam.digital.data.platform.restapi.core.exception.KafkaConstraintViolationException;
import com.epam.digital.data.platform.restapi.core.exception.KafkaSecurityValidationFailedException;
import com.epam.digital.data.platform.restapi.core.exception.KafkaInternalServerException;
import com.epam.digital.data.platform.restapi.core.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.function.BiFunction;

public final class ResponseResolverUtil {

  private static final Map<HttpStatus, BiFunction<Response<?>, HttpStatus, RuntimeException>>
      httpErrorCodeToException =
          Map.of(
              HttpStatus.INTERNAL_SERVER_ERROR,
                  (response, httpStatus) ->
                      new KafkaInternalServerException(
                          "Kafka returned error response", response, httpStatus),
              HttpStatus.CONFLICT,
                  (response, httpStatus) ->
                      new KafkaConstraintViolationException(
                          "DB constraint violation in kafka processing", response, httpStatus),
              HttpStatus.FORBIDDEN,
                  (response, httpStatus) ->
                      new KafkaSecurityValidationFailedException(
                          "One of security validations failed for operation on kafka side", response, httpStatus),
              HttpStatus.NOT_FOUND,
                  (response, httpStatus) -> new NotFoundException(response.getDetails()));

  private ResponseResolverUtil() {
  }

  public static <T> ResponseEntity<T> getHttpResponseFromKafka(Response<T> kafkaResponse) {
    HttpStatus httpStatus = StatusUtils.convertResponseStatus(kafkaResponse);
    if (httpErrorCodeToException.containsKey(httpStatus)) {
      throw httpErrorCodeToException.get(httpStatus)
          .apply(kafkaResponse, httpStatus);
    }
    return ResponseEntity.status(httpStatus)
        .body(kafkaResponse.getPayload());
  }
}
