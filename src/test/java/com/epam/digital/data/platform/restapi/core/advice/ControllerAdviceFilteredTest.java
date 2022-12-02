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

package com.epam.digital.data.platform.restapi.core.advice;

import static com.epam.digital.data.platform.restapi.core.util.ControllerTestUtils.mockResponse;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_ACCESS_TOKEN;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE_DERIVED;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_APPLICATION;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_PROCESS;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_PROCESS_DEFINITION_ID;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_SYSTEM;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.config.SecurityConfiguration;
import com.epam.digital.data.platform.restapi.core.config.TestBeansConfig;
import com.epam.digital.data.platform.restapi.core.config.WebConfigProperties;
import com.epam.digital.data.platform.restapi.core.controller.MockFileController;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import com.epam.digital.data.platform.restapi.core.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.restapi.core.exception.ChecksumInconsistencyException;
import com.epam.digital.data.platform.restapi.core.exception.KafkaCephResponseNotFoundException;
import com.epam.digital.data.platform.restapi.core.filter.FilterChainExceptionHandler;
import com.epam.digital.data.platform.restapi.core.model.FileProperty;
import com.epam.digital.data.platform.restapi.core.service.DigitalSignatureService;
import com.epam.digital.data.platform.restapi.core.service.FileService;
import com.epam.digital.data.platform.restapi.core.service.MockFileService;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.restapi.core.utils.ResponseCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MockFileController.class)
@ContextConfiguration(
    classes = {
        FilterChainExceptionHandler.class,
        ApplicationExceptionHandler.class,
        FileRequestBodyAspect.class
    })
@Import({TestBeansConfig.class, AopAutoConfiguration.class})
@TestPropertySource(properties = {
    "data-platform.signature.validation.enabled=false",
    "data-platform.files.processing.enabled=true"
})
@SecurityConfiguration
class ControllerAdviceFilteredTest {

  private static final String BASE_URL = "/mock-file";
  private static final UUID ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  private static final HttpHeaders MANDATORY_HEADERS = new HttpHeaders();

  private static final String TRACE_ID = "1";
  private static final String ACCESS_TOKEN;
  private static final String SIGNATURE = "some signature";

  static {
    try {
      ACCESS_TOKEN = new String(IOUtils.toByteArray(
          ControllerAdviceFilteredTest.class.getResourceAsStream("/accessToken.json")));

      MANDATORY_HEADERS.add(X_ACCESS_TOKEN.getHeaderName(), ACCESS_TOKEN);
      MANDATORY_HEADERS.add(X_DIGITAL_SIGNATURE.getHeaderName(), "SomeDS");
      MANDATORY_HEADERS.add(X_DIGITAL_SIGNATURE_DERIVED.getHeaderName(), "SomeDSD");
      MANDATORY_HEADERS.add(X_SOURCE_SYSTEM.getHeaderName(), "SomeSS");
      MANDATORY_HEADERS.add(X_SOURCE_APPLICATION.getHeaderName(), "SomeSA");
      MANDATORY_HEADERS.add(X_SOURCE_BUSINESS_PROCESS.getHeaderName(), "SomeBP");
      MANDATORY_HEADERS.add(X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName(), "SomeIId");
      MANDATORY_HEADERS.add(X_SOURCE_BUSINESS_PROCESS_DEFINITION_ID.getHeaderName(), "SomeDId");
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @Autowired
  MockMvc mockMvc;
  @Autowired
  ObjectMapper objectMapper;

  @MockBean
  MockFileService mockFileService;
  @MockBean
  FileService fileService;

  @MockBean
  DigitalSignatureService digitalSignatureService;
  @MockBean
  TraceProvider traceProvider;
  @MockBean
  WebConfigProperties webConfigProperties;

  @BeforeEach
  void beforeEach() {
    when(traceProvider.getRequestId()).thenReturn(TRACE_ID);
    when(digitalSignatureService.copySignature(any())).thenReturn(SIGNATURE);

    var filtersExclude = new WebConfigProperties.Filters();
    filtersExclude.setExclude(List.of("/"));
    when(webConfigProperties.getFilters()).thenReturn(filtersExclude);

    when(fileService.getFileProperties(any())).thenReturn(List.of(
        new FileProperty("scanCopy", new File())
    ));
    when(fileService.store(any(), any())).thenReturn(true);
    when(fileService.retrieve(any(), any())).thenReturn(true);
  }

  @Test
  void storeFilesOnPost() throws Exception {
    when(mockFileService.create(any()))
        .thenReturn(mockResponse(Status.CREATED));

    mockMvc.perform(post(BASE_URL)
        .headers(MANDATORY_HEADERS)
        .contentType(MediaType.APPLICATION_JSON)
        .content("{}"))
        .andExpect(status().isCreated());

    verify(fileService).store(any(), any());
  }

  @Test
  void storeFilesOnPut() throws Exception {
    when(mockFileService.update(any()))
            .thenReturn(mockResponse(Status.NO_CONTENT));

    mockMvc.perform(put(BASE_URL + "/{id}", ID)
            .headers(MANDATORY_HEADERS)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isNoContent());

    verify(fileService).store(any(), any());
  }

  @Test
  void storeFilesOnPatch() throws Exception {
    when(mockFileService.update(any()))
            .thenReturn(mockResponse(Status.NO_CONTENT));

    mockMvc.perform(patch(BASE_URL + "/{id}", ID)
            .headers(MANDATORY_HEADERS)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{}"))
            .andExpect(status().isNoContent());

    verify(fileService).store(any(), any());
  }

  @Test
  void shouldNotStoreFilesWhenInputValidationFailed() throws Exception {
    mockMvc
        .perform(
            post(BASE_URL)
                .headers(MANDATORY_HEADERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"someField\":\"long long String\"}"))
        .andExpectAll(
            status().isUnprocessableEntity(),
            jsonPath("$.code").value(is(ResponseCode.VALIDATION_ERROR)));

    verify(fileService, never()).store(any(), any());
  }

  @Test
  void shouldNotStoreFileWhenItWasChanged() throws Exception {
    when(mockFileService.create(any())).thenThrow(new ChecksumInconsistencyException(""));
    mockMvc
        .perform(
            post(BASE_URL)
                .headers(MANDATORY_HEADERS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
        .andExpectAll(
                status().isInternalServerError(),
                jsonPath("$.traceId").value(is(TRACE_ID)),
                jsonPath("$.code").value(is(ResponseCode.FILE_WAS_CHANGED)));
  }

  @Test
  void shouldReturn500WhenKafkaCephResponseIsNotFound() throws Exception {
    when(mockFileService.read(any())).thenThrow(new KafkaCephResponseNotFoundException(""));

    mockMvc
        .perform(get(BASE_URL + "/{id}", ID).headers(MANDATORY_HEADERS))
        .andExpectAll(
                status().isInternalServerError(),
                jsonPath("$.traceId").value(is(TRACE_ID)),
                jsonPath("$.code").value(is(ResponseCode.INTERNAL_CONTRACT_VIOLATION)));
  }

  @Test
  void retrieveFilesOnGet() throws Exception {
    when(mockFileService.read(any()))
        .thenReturn(mockResponse(Status.SUCCESS, new MockEntityFile()));

    mockMvc.perform(get(BASE_URL + "/{id}", ID)
        .headers(MANDATORY_HEADERS))
        .andExpect(status().isOk());

    verify(fileService).retrieve(any(), any());
  }
}
