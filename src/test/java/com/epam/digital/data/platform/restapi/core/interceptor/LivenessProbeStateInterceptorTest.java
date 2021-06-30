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

package com.epam.digital.data.platform.restapi.core.interceptor;

import com.epam.digital.data.platform.restapi.core.config.SecurityConfiguration;
import com.epam.digital.data.platform.restapi.core.config.WebConfig;
import com.epam.digital.data.platform.restapi.core.controller.MockController;
import com.epam.digital.data.platform.restapi.core.service.MockService;
import com.epam.digital.data.platform.starter.security.PermitAllWebSecurityConfig;
import com.epam.digital.data.platform.starter.security.config.SecurityProperties;
import com.epam.digital.data.platform.starter.security.jwt.TokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import com.epam.digital.data.platform.starter.actuator.livenessprobe.LivenessStateHandler;

import java.util.UUID;

import static com.epam.digital.data.platform.restapi.core.util.ControllerTestUtils.mockSuccessResponse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest
@ContextConfiguration(classes = {MockController.class,
        LivenessProbeStateInterceptor.class, WebConfig.class})
@SecurityConfiguration
class LivenessProbeStateInterceptorTest {

  private static final String BASE_URL = "/mock";
  private static final UUID CONSENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private MockService mockService;
  @MockBean
  private LivenessStateHandler livenessStateHandler;
  @MockBean
  private SecurityProperties securityProperties;

  @Test
  void expectStateHandlerIsCalledInInterceptor() throws Exception {
    when(mockService.read(any())).thenReturn(mockSuccessResponse());

    mockMvc.perform(get(BASE_URL + "/{id}", CONSENT_ID));

    verify(livenessStateHandler).handleResponse(eq(HttpStatus.OK), any());
  }
}
