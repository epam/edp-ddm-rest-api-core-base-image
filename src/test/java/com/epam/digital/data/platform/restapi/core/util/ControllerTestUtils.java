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

package com.epam.digital.data.platform.restapi.core.util;

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.model.FieldsValidationErrorDetails;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class ControllerTestUtils {

  public static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
      .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

  private ControllerTestUtils() {
  }

  public static <T> Response<T> mockSuccessResponse() {
    Response<T> response = new Response<>();
    response.setStatus(Status.SUCCESS);
    return response;
  }

  public static <T> Response<T> mockSuccessResponse(T payload) {
    Response<T> response = new Response<>();
    response.setStatus(Status.SUCCESS);
    response.setPayload(payload);
    return response;
  }

  public static <T> Response<T> mockResponse(Status status) {
    Response<T> response = new Response<>();
    response.setStatus(status);
    return response;
  }

  public static <T> Response<T> mockResponse(Status status, T payload) {
    Response<T> response = new Response<>();
    response.setStatus(status);
    response.setPayload(payload);
    return response;
  }

  public static FieldsValidationErrorDetails validationDetailsFrom(
      FieldsValidationErrorDetails.FieldError... details) {
    return new FieldsValidationErrorDetails(Arrays.asList(details));
  }

  public static MockEntity getMockEntity() {
    MockEntity mockEntity = new MockEntity();
    mockEntity.setConsentDate(LocalDateTime.of(2021, 2, 27, 10, 5));
    mockEntity.setPersonFullName("John Doe");
    mockEntity.setPersonPassNumber("АА112233");
    return mockEntity;
  }
}
