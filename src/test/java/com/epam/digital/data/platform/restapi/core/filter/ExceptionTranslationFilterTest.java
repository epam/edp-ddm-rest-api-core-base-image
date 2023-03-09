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

package com.epam.digital.data.platform.restapi.core.filter;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.restapi.core.audit.RestAuditEventsFacade;
import com.epam.digital.data.platform.restapi.core.config.UnauthorizedRequestHandler;
import com.epam.digital.data.platform.restapi.core.controller.impl.MockController;
import com.epam.digital.data.platform.restapi.core.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.restapi.core.service.MockService;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.restapi.core.utils.ResponseCode;
import com.epam.digital.data.platform.starter.security.WebSecurityConfig;
import com.epam.digital.data.platform.starter.security.config.SecurityProperties;
import com.epam.digital.data.platform.starter.security.config.Whitelist;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import com.epam.digital.data.platform.starter.security.jwt.DefaultAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@ContextConfiguration(classes = {MockController.class, UnauthorizedRequestHandler.class,
    WebSecurityConfig.class,
    Whitelist.class, SecurityProperties.class, ApplicationExceptionHandler.class})
@ComponentScan(basePackages = {"com.epam.digital.data.platform.starter.security.jwt"},
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = DefaultAuthenticationEntryPoint.class)})
@Import({Whitelist.class})
class ExceptionTranslationFilterTest {

  private static final String BASE_URL = "/mock";
  private static final String TRACE_ID = "1";
  private static final UUID ENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  @Autowired
  private MockMvc mockMvc;
  @Autowired
  private ObjectMapper objectMapper;
  @MockBean
  private MockService mockService;
  @MockBean
  private TraceProvider traceProvider;
  @MockBean
  private RestAuditEventsFacade restAuditEventsFacade;
  @MockBean
  private SecurityProperties securityProperties;

  @BeforeEach
  void beforeEach() {
    when(traceProvider.getRequestId()).thenReturn(TRACE_ID);
  }

  @Test
  void shouldReturn401WhenNoJwtProvided() throws Exception {
    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpect(status().isUnauthorized())
        .andExpectAll(
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.AUTHENTICATION_FAILED)),
            jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturn401WhenJwtParsingException() throws Exception {
    when(mockService.read(any()))
        .thenThrow(JwtParsingException.class);

    mockMvc.perform(get(BASE_URL + "/{id}", ENTITY_ID))
        .andExpectAll(
            status().isUnauthorized(),
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.code").value(is(ResponseCode.AUTHENTICATION_FAILED)),
            jsonPath("$.details").doesNotExist());
  }
}
