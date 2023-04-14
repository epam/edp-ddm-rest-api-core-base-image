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

import static com.epam.digital.data.platform.restapi.core.utils.Header.X_ACCESS_TOKEN;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE_DERIVED;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_APPLICATION;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_PROCESS;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_SYSTEM;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.epam.digital.data.platform.dso.client.exception.InvalidSignatureException;
import com.epam.digital.data.platform.integration.ceph.exception.CephCommunicationException;
import com.epam.digital.data.platform.integration.ceph.exception.MisconfigurationException;
import com.epam.digital.data.platform.restapi.core.config.SecurityConfiguration;
import com.epam.digital.data.platform.restapi.core.config.TestBeansConfig;
import com.epam.digital.data.platform.restapi.core.config.WebConfigProperties;
import com.epam.digital.data.platform.restapi.core.filter.FilterChainExceptionHandler;
import com.epam.digital.data.platform.restapi.core.service.DigitalSignatureService;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.restapi.core.utils.ResponseCode;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FilterChainExceptionHandler.class)
@ContextConfiguration(
    classes = {FilterChainExceptionHandler.class, ApplicationExceptionHandler.class})
@Import(TestBeansConfig.class)
@TestPropertySource(properties = {
    "data-platform.signature.validation.enabled=true"
})
@SecurityConfiguration
class ApplicationExceptionHandlerFilteredTest {

  private static final String BASE_URL = "/mock";
  private static final UUID CONSENT_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  private static final HttpHeaders MANDATORY_HEADERS = new HttpHeaders();

  private static final String TRACE_ID = "1";
  private static final String ACCESS_TOKEN;
  private static final String ACCESS_TOKEN_WITHOUT_DRFO;

  static {
    try {
      ACCESS_TOKEN = new String(IOUtils.toByteArray(
          ApplicationExceptionHandlerFilteredTest.class.getResourceAsStream("/accessToken.json")));
      ACCESS_TOKEN_WITHOUT_DRFO = new String(IOUtils.toByteArray(
          ApplicationExceptionHandlerFilteredTest.class
              .getResourceAsStream("/accessTokenWithoutDrfo.json")));

      MANDATORY_HEADERS.add(X_ACCESS_TOKEN.getHeaderName(), ACCESS_TOKEN);
      MANDATORY_HEADERS.add(X_DIGITAL_SIGNATURE.getHeaderName(), "SomeDS");
      MANDATORY_HEADERS.add(X_DIGITAL_SIGNATURE_DERIVED.getHeaderName(), "SomeDSD");
      MANDATORY_HEADERS.add(X_SOURCE_SYSTEM.getHeaderName(), "SomeSS");
      MANDATORY_HEADERS.add(X_SOURCE_APPLICATION.getHeaderName(), "SomeSA");
      MANDATORY_HEADERS.add(X_SOURCE_BUSINESS_PROCESS.getHeaderName(), "SomeBP");
    } catch (IOException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private TraceProvider traceProvider;
  @MockBean
  private DigitalSignatureService digitalSignatureService;
  @MockBean
  private WebConfigProperties webConfigProperties;

  @BeforeEach
  void beforeEach() {
    when(traceProvider.getRequestId()).thenReturn(TRACE_ID);

    var filtersExclude = new WebConfigProperties.Filters();
    filtersExclude.setExclude(List.of("/"));
    when(webConfigProperties.getFilters()).thenReturn(filtersExclude);
  }

  @Test
  void shouldReturn412WhenSignatureIsNotValid() throws Exception {
    doThrow(InvalidSignatureException.class).when(digitalSignatureService)
        .checkSignature(any(), any());
    mockMvc.perform(delete(BASE_URL + "/{id}", CONSENT_ID)
        .headers(MANDATORY_HEADERS))
        .andExpectAll(
            status().isPreconditionFailed(),
            jsonPath("$.code").value(is(ResponseCode.SIGNATURE_VIOLATION)),
            jsonPath("$.traceId").value(is(TRACE_ID)),
            jsonPath("$.details").doesNotExist()
        );
  }

  @Test
  void shouldReturn500WhenCephBucketNotFoundException() throws Exception {
    doThrow(MisconfigurationException.class)
        .when(digitalSignatureService).checkSignature(any(), any());

    mockMvc.perform(delete(BASE_URL + "/{id}", CONSENT_ID)
        .headers(MANDATORY_HEADERS))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(ResponseCode.INTERNAL_CONTRACT_VIOLATION))
        .andExpect(jsonPath("$.traceId").value(is(TRACE_ID)))
        .andExpect(jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturn500WhenCephCommunicationException() throws Exception {
    doThrow(CephCommunicationException.class)
        .when(digitalSignatureService).checkSignature(any(), any());

    mockMvc.perform(delete(BASE_URL + "/{id}", CONSENT_ID)
        .headers(MANDATORY_HEADERS))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(ResponseCode.THIRD_PARTY_SERVICE_UNAVAILABLE))
        .andExpect(jsonPath("$.traceId").value(is(TRACE_ID)))
        .andExpect(jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturn400WhenDigitalSignatureIsNotFound() throws Exception {
    doThrow(new DigitalSignatureNotFoundException(""))
        .when(digitalSignatureService).checkSignature(any(), any());

    mockMvc.perform(delete(BASE_URL + "/{id}", CONSENT_ID)
        .headers(MANDATORY_HEADERS))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_HEADER_VALUE))
        .andExpect(jsonPath("$.traceId").value(is(TRACE_ID)))
        .andExpect(jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturn400WhenMandatoryClaimAbsentInAccessToken() throws Exception {
    mockMvc.perform(delete(BASE_URL + "/{id}", CONSENT_ID)
        .header(X_ACCESS_TOKEN.getHeaderName(), ACCESS_TOKEN_WITHOUT_DRFO)
        .header(X_DIGITAL_SIGNATURE.getHeaderName(), "SomeDS")
        .header(X_DIGITAL_SIGNATURE_DERIVED.getHeaderName(), "SomeDSD")
        .header(X_SOURCE_SYSTEM.getHeaderName(), "SomeSS")
        .header(X_SOURCE_BUSINESS_PROCESS.getHeaderName(), "SomeBP")
        .header(X_SOURCE_APPLICATION.getHeaderName(), "SomeSA"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_HEADER_VALUE))
        .andExpect(jsonPath("$.traceId").value(is(TRACE_ID)))
        .andExpect(jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturn500WhenKepServiceInternalServerErrorException() throws Exception {
    doThrow(KepServiceInternalServerErrorException.class)
        .when(digitalSignatureService).checkSignature(any(), any());

    mockMvc.perform(delete(BASE_URL + "/{id}", CONSENT_ID)
        .headers(MANDATORY_HEADERS))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(ResponseCode.THIRD_PARTY_SERVICE_UNAVAILABLE))
        .andExpect(jsonPath("$.traceId").value(is(TRACE_ID)))
        .andExpect(jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturn500WhenKepServiceBadRequestException() throws Exception {
    doThrow(KepServiceBadRequestException.class)
        .when(digitalSignatureService).checkSignature(any(), any());

    mockMvc.perform(delete(BASE_URL + "/{id}", CONSENT_ID)
        .headers(MANDATORY_HEADERS))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value(ResponseCode.INTERNAL_CONTRACT_VIOLATION))
        .andExpect(jsonPath("$.traceId").value(is(TRACE_ID)))
        .andExpect(jsonPath("$.details").doesNotExist());
  }

  @Test
  void shouldReturn400WhenMandatoryHeaderMissed() throws Exception {
    mockMvc.perform(delete(BASE_URL + "/{id}", CONSENT_ID)
        .header(X_ACCESS_TOKEN.getHeaderName(), ACCESS_TOKEN))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value(is(ResponseCode.HEADERS_ARE_MISSING)))
        .andExpect(jsonPath("$.traceId").value(is(TRACE_ID)))
        .andExpect(jsonPath("$.details").doesNotExist());
  }
}
