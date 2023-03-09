/*
 * Copyright 2023 EPAM Systems.
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

package com.epam.digital.data.platform.restapi.core.controller;

import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.config.SecurityConfiguration;
import com.epam.digital.data.platform.restapi.core.config.TestBeansConfig;
import com.epam.digital.data.platform.restapi.core.controller.impl.MockController;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.filter.DigitalSignatureValidationFilter;
import com.epam.digital.data.platform.restapi.core.filter.HeaderValidationFilter;
import com.epam.digital.data.platform.restapi.core.service.MockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.epam.digital.data.platform.restapi.core.util.ControllerTestUtils.DATE_TIME_FORMATTER;
import static com.epam.digital.data.platform.restapi.core.util.ControllerTestUtils.mockResponse;
import static com.epam.digital.data.platform.restapi.core.util.ControllerTestUtils.mockSuccessResponse;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = {DigitalSignatureValidationFilter.class, HeaderValidationFilter.class})
    })
@ContextConfiguration(classes = {MockController.class, TestBeansConfig.class})
@SecurityConfiguration
public class MockControllerTest {

  private static final String BASE_URL = "/mock";
  private static final UUID MOCK_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  @Autowired
  private MockMvc mockMvc;
  @MockBean
  private MockService mockService;

  @Test
  void expectValidMockEntityById() throws Exception {
    Response<MockEntity> response = mockSuccessResponse(mockPayload(MOCK_ID));
    when(mockService.read(any())).thenReturn(response);

    mockMvc
        .perform(get(BASE_URL + "/{id}", MOCK_ID))
        .andExpectAll(
            status().isOk(),
            content().contentType(MediaType.APPLICATION_JSON),
            jsonPath("$.consentId", is(MOCK_ID.toString())),
            jsonPath("$.consentDate", is("2012-11-27T10:45:12.123Z")));
  }

  @Test
  void expectMockEntityIsCreated() throws Exception {
    when(mockService.create(any())).thenReturn(mockResponse(Status.CREATED));

    mockMvc
        .perform(
            post(BASE_URL)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(
                    "{\"consentDate\": \"2012-12-24T15:33:50.678Z\","
                        + "\"personFullName\":\"Name\","
                        + "\"personPassNumber\": \"АВ334455\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void expectMockEntityListIsCreated() throws Exception {
    when(mockService.createList(any())).thenReturn(mockResponse(Status.CREATED));

    mockMvc
        .perform(
            post(BASE_URL + "/list")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(
                    "{\"entities\":[{\"consentDate\": \"2012-12-24T15:33:50.678Z\","
                        + "\"personFullName\":\"Name\","
                        + "\"personPassNumber\": \"АВ334455\"}]}"))
        .andExpect(status().isCreated());
  }

  @Test
  void expectValidMockEntityUpdated() throws Exception {
    Response<Void> response = new Response<>();
    response.setStatus(Status.NO_CONTENT);
    when(mockService.update(any())).thenReturn(response);

    mockMvc
        .perform(
            put(BASE_URL + "/{id}", MOCK_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(
                    "{\"consentDate\": \"2012-12-24T15:33:50.678Z\","
                        + "\"personFullName\":\"Name\","
                        + "\"personPassNumber\": \"АВ334455\"}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void expectValidMockEntityUpdateWithoutFields() throws Exception {
    Response<Void> response = new Response<>();
    response.setStatus(Status.NO_CONTENT);
    when(mockService.update(any())).thenReturn(response);

    mockMvc
        .perform(
            put(BASE_URL + "/{id}", MOCK_ID)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content("{}"))
        .andExpect(status().isNoContent());
  }

  @Test
  void expectMockEntityDeleted() throws Exception {
    Response<Void> response = new Response<>();
    response.setStatus(Status.NO_CONTENT);
    when(mockService.delete(any())).thenReturn(response);

    mockMvc.perform(delete(BASE_URL + "/{id}", MOCK_ID)).andExpect(status().isNoContent());
  }

  private MockEntity mockPayload(UUID id) {
    MockEntity stub = new MockEntity();
    stub.setConsentId(id);
    stub.setPersonPassNumber("АВ334455");
    stub.setPersonFullName("Name");
    stub.setConsentDate(LocalDateTime.parse("2012-11-27T10:45:12.123Z", DATE_TIME_FORMATTER));
    return stub;
  }
}
