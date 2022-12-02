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

package com.epam.digital.data.platform.restapi.core.exception;

import static com.epam.digital.data.platform.restapi.core.util.ControllerTestUtils.mockResponse;
import static com.epam.digital.data.platform.restapi.core.util.ControllerTestUtils.validationDetailsFrom;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.audit.RestAuditEventsFacade;
import com.epam.digital.data.platform.restapi.core.config.SecurityConfiguration;
import com.epam.digital.data.platform.restapi.core.controller.MockController;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityCreateList;
import com.epam.digital.data.platform.restapi.core.model.DetailedErrorResponse;
import com.epam.digital.data.platform.restapi.core.model.FieldsValidationErrorDetails;
import com.epam.digital.data.platform.restapi.core.service.MockService;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.restapi.core.utils.ResponseCode;
import com.epam.digital.data.platform.starter.security.config.SecurityProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@WebMvcTest
@ContextConfiguration(classes = {MockController.class, ApplicationExceptionHandler.class})
@SecurityConfiguration
class ApplicationExceptionHandlerTest extends ResponseEntityExceptionHandler {

  private static final String BASE_URL = "/mock";
  private static final UUID ENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  private static final String TRACE_ID = "1";

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private MockService mockService;
  @MockBean
  private RestAuditEventsFacade restAuditEventsFacade;
  @MockBean
  private TraceProvider traceProvider;
  @MockBean
  private SecurityProperties securityProperties;

  @BeforeEach
  void beforeEach() {
    when(traceProvider.getRequestId()).thenReturn(TRACE_ID);
  }

  @Test
  void shouldReturnTimeoutErrorOnNoKafkaResponse() throws Exception {
    when(mockService.read(any())).thenThrow(NoKafkaResponseException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isInternalServerError())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof NoKafkaResponseException))
        .andExpectAll(
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.TIMEOUT_ERROR)),
            jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturnRuntimeErrorOnSqlErrorException() throws Exception {
    when(mockService.read(any())).thenThrow(SqlErrorException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isInternalServerError())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof SqlErrorException))
        .andExpectAll(
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.RUNTIME_ERROR)),
            jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturnForbiddenOperationOnException() throws Exception {
    when(mockService.read(any())).thenThrow(ForbiddenOperationException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isForbidden())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof ForbiddenOperationException))
        .andExpectAll(
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.FORBIDDEN_OPERATION)),
            jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturnRuntimeErrorOnGenericException() throws Exception {
    when(mockService.read(any())).thenThrow(RuntimeException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isInternalServerError())
        .andExpectAll(
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.RUNTIME_ERROR)),
            jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturnBadRequestOnHttpNotReadable() throws Exception {
    when(mockService.read(any())).thenThrow(HttpMessageNotReadableException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isBadRequest())
        .andExpect(response -> assertTrue(
            response.getResolvedException() instanceof HttpMessageNotReadableException));
  }

  @Test
  void shouldReturn415WithBodyWhenMediaTypeIsNotSupported() throws Exception {
    var unsupportedMediaType = MediaType.APPLICATION_PDF;

    mockMvc.perform(post(BASE_URL)
            .contentType(unsupportedMediaType))
        .andExpectAll(
            status().isUnsupportedMediaType(),
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.UNSUPPORTED_MEDIA_TYPE)),
            jsonPath("$.details").doesNotExist()
        );
  }

  @Test
  void shouldReturn500WithCorrectCodeWhenKafkaInternalException() throws Exception {
    when(mockService.read(any()))
        .thenReturn(mockResponse(Status.THIRD_PARTY_SERVICE_UNAVAILABLE));

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isInternalServerError())
        .andExpectAll(jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is("THIRD_PARTY_SERVICE_UNAVAILABLE")),
            jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturn409WithDetailsWhenConstraintException() throws Exception {
    Response<MockEntity> kafkaResponse = mockResponse(Status.CONSTRAINT_VIOLATION);
    kafkaResponse.setDetails("not null");
    when(mockService.read(any()))
        .thenReturn(kafkaResponse);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isConflict())
        .andExpectAll(jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is("CONSTRAINT_ERROR")),
            jsonPath("$.details.constraint").value(is("not null")));
  }

  @Test
  void shouldReturn400WithDetailsWhenFileNotExistsException() throws Exception {
    when(mockService.create(any()))
        .thenThrow(new FileNotExistsException("", Collections.singletonList("scanCopy")));

    var inputBody = new MockEntity();
    inputBody.setPersonFullName("Valid Name");
    String inputStringBody = objectMapper.writeValueAsString(inputBody);

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputStringBody))
        .andExpect(status().isBadRequest())
        .andExpectAll(
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is("FILE_NOT_FOUND")),
            jsonPath("$.details[0]").value(is("scanCopy")));
  }

  @Test
  void shouldReturn422WithBodyWhenMethodArgumentNotValid() throws Exception {
    var inputBody = new MockEntity();
    inputBody.setPersonPassNumber("AA12345");
    inputBody.setPersonFullName("Valid Name");
    String inputStringBody = objectMapper.writeValueAsString(inputBody);

    var expectedResponseObject = new DetailedErrorResponse<FieldsValidationErrorDetails>();
    expectedResponseObject.setTraceId(TRACE_ID);
    expectedResponseObject.setCode(ResponseCode.VALIDATION_ERROR);
    expectedResponseObject.setDetails(
        validationDetailsFrom(new FieldsValidationErrorDetails.FieldError(
            "AA12345", "personPassNumber", "must match \"^[АВЕІКМНОРСТХ]{2}[0-9]{6}$\"")));
    String expectedOutputBody = objectMapper.writeValueAsString(expectedResponseObject);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content(inputStringBody))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(response ->
            assertTrue(response.getResolvedException() instanceof MethodArgumentNotValidException))
        .andExpect(content().json(expectedOutputBody));
  }

  @Test
  void shouldReturn422WithBodyWhenSizeExceeded() throws Exception {
    var inputBodyEntry = new MockEntity();
    inputBodyEntry.setPersonFullName("Valid Name");

    var inputBody = new MockEntityCreateList();
    inputBody.setEntities(new MockEntity[]{ inputBodyEntry, inputBodyEntry, inputBodyEntry });
    String inputStringBody = objectMapper.writeValueAsString(inputBody);

    mockMvc.perform(post(BASE_URL + "/list")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(inputStringBody))
            .andExpectAll(status().isUnprocessableEntity(),
                    response ->
                            assertTrue(response.getResolvedException() instanceof MethodArgumentNotValidException),
                    jsonPath("$.code", is(ResponseCode.LIST_SIZE_VALIDATION_ERROR)));
  }

  @Test
  void shouldReturn422WithBodyWhenDateTimeArgNotValid() throws Exception {
    var expectedResponseObject = new DetailedErrorResponse<FieldsValidationErrorDetails>();
    expectedResponseObject.setTraceId(TRACE_ID);
    expectedResponseObject.setCode(ResponseCode.VALIDATION_ERROR);
    expectedResponseObject.setDetails(
        validationDetailsFrom(new FieldsValidationErrorDetails.FieldError(
            "invalid date", "consentDate", "Text 'invalid date' could not be parsed at index 0")));
    String expectedOutputBody = objectMapper.writeValueAsString(expectedResponseObject);

    mockMvc.perform(post(BASE_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"consentDate\": \"invalid date\"}"))
        .andExpect(status().isUnprocessableEntity())
        .andExpect(content().json(expectedOutputBody));
  }

  @Test
  void shouldReturn400WithBodyWhenPathArgumentIsNotValid() throws Exception {
    mockMvc.perform(get(BASE_URL + "/invalidUUID"))
        .andExpectAll(
            status().isBadRequest(),
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.METHOD_ARGUMENT_TYPE_MISMATCH)),
            jsonPath("$.details").doesNotExist()
        );
  }

  @Test
  void shouldReturn403WhenKafkaReturnJwtError() throws Exception {
    when(mockService.read(any()))
        .thenReturn(mockResponse(Status.JWT_INVALID));

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpectAll(
            status().isForbidden(),
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.JWT_INVALID)));
  }

  @Test
  void shouldReturn403WhenForbiddenOperation() throws Exception {
    when(mockService.read(any())).thenReturn(mockResponse(Status.FORBIDDEN_OPERATION));

    mockMvc
        .perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpectAll(
            status().isForbidden(),
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.FORBIDDEN_OPERATION)));
  }

  @Test
  void shouldReturn404WhenNotFoundException() throws Exception {
    when(mockService.read(any()))
        .thenThrow(new NotFoundException("some resource is not found"));

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpectAll(
            status().isNotFound(),
            jsonPath("$.traceId").value(is("1")),
            jsonPath("$.code").value(is(ResponseCode.NOT_FOUND)));
  }
}
