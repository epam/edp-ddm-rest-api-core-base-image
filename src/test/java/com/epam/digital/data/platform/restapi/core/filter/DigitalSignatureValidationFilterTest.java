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

import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE_DERIVED;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.dso.client.exception.InvalidSignatureException;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.config.WebConfigProperties;
import com.epam.digital.data.platform.restapi.core.service.DigitalSignatureService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class DigitalSignatureValidationFilterTest {

  private static final String X_DIGITAL_SIGNATURE_VALUE = "x-digital-signature-header-value";
  private static final String X_DIGITAL_SIGNATURE_DERIVED_VALUE = "x-digital-signature-derived-header-value";

  private static final String uuid = "4b57a8d7-1af0-4f0a-8afc-dfc078299885";
  private static final String deleteData = String.format("{\"id\":\"%s\"}", uuid);
  private static final String URL = "/some/service/" + uuid;
  private static final String REQUEST_BODY = "some request body";

  private static final String SIGNATURE_CEPH_OBJECT = "digital_signature_ceph_object";
  private static final String SIGNATURE_DERIVED_CEPH_OBJECT = "digital_signature_derived_ceph_object";
  private static final String HASH_OF_EMPTY_STRING = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

  private Filter filter;

  @Mock
  private HttpServletRequest request;
  @Mock
  private HttpServletResponse response;
  @Mock
  private FilterChain filterChain;
  @Mock
  private DigitalSignatureService digitalSignatureService;
  private ObjectMapper mapper = new ObjectMapper();

  private WebConfigProperties webConfigProperties;
  @Captor
  private ArgumentCaptor<SecurityContext> securityContextCaptor;
  @Captor
  private ArgumentCaptor<ServletRequest> requestCaptor;

  private SecurityContext securityContext;

  @BeforeEach
  void init() throws IOException {
    webConfigProperties = new WebConfigProperties();
    webConfigProperties.setFilters(new WebConfigProperties.Filters());
    webConfigProperties.getFilters().setExclude(List.of("/path"));

    filter =
        new DigitalSignatureValidationFilter(
            webConfigProperties, digitalSignatureService, mapper, true);

    when(digitalSignatureService.copySignature(any())).thenReturn("");

    when(request.getHeader(X_DIGITAL_SIGNATURE.getHeaderName()))
        .thenReturn(X_DIGITAL_SIGNATURE_VALUE);
    when(request.getHeader(X_DIGITAL_SIGNATURE_DERIVED.getHeaderName()))
        .thenReturn(X_DIGITAL_SIGNATURE_DERIVED_VALUE);

    when(request.getRequestURI()).thenReturn(URL);
    when(request.getContextPath()).thenReturn("/");
    when(request.getPathInfo()).thenReturn(URL);
    when(request.getMethod()).thenReturn("DELETE");
    when(request.getReader()).thenReturn(new BufferedReader(new StringReader(REQUEST_BODY)));

    securityContext = new SecurityContext();
    securityContext.setDigitalSignature(X_DIGITAL_SIGNATURE_VALUE);
    securityContext.setDigitalSignatureChecksum(HASH_OF_EMPTY_STRING);
    securityContext.setDigitalSignatureDerived(X_DIGITAL_SIGNATURE_DERIVED_VALUE);
    securityContext.setDigitalSignatureDerivedChecksum(HASH_OF_EMPTY_STRING);
  }

  @Test
  void methodGet() throws IOException, ServletException {
    securityContext = new SecurityContext();
    when(request.getMethod()).thenReturn("GET");
    filter.doFilter(request, response, filterChain);

    verify(request).setAttribute(SecurityContext.class.getSimpleName(), securityContext);
    verify(digitalSignatureService, never()).checkSignature(any(), any());
  }

  @Test
  void methodDelete() throws IOException, ServletException {
    filter.doFilter(request, response, filterChain);

    verify(digitalSignatureService).checkSignature(deleteData, securityContext);
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH"})
  void methodPostBody(String arg) throws IOException, ServletException {
    when(request.getMethod()).thenReturn(arg);

    filter.doFilter(request, response, filterChain);

    verify(digitalSignatureService).checkSignature(REQUEST_BODY, securityContext);
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH"})
  void methodPostMultiReadRequest(String arg) throws IOException, ServletException {
    when(request.getMethod()).thenReturn(arg);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
    assertEquals(MultiReadHttpServletRequest.class, requestCaptor.getValue().getClass());
  }

  @Test
  void missingXDigitalSignature() {
    when(request.getHeader(X_DIGITAL_SIGNATURE.getHeaderName())).thenReturn(null);

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> filter.doFilter(request, response, filterChain));
  }

  @Test
  void missingXDigitalSignatureDerived() {
    when(request.getHeader(X_DIGITAL_SIGNATURE_DERIVED.getHeaderName())).thenReturn(null);

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> filter.doFilter(request, response, filterChain));
  }

  @Test
  void invalidSignature() {
    securityContext.setDigitalSignatureChecksum(null);
    securityContext.setDigitalSignatureDerivedChecksum(null);
    doThrow(InvalidSignatureException.class)
        .when(digitalSignatureService).checkSignature(deleteData, securityContext);

    Assertions.assertThrows(InvalidSignatureException.class,
        () -> filter.doFilter(request, response, filterChain));
  }

  @Test
  void ignoringSlashesAtTheEndOfTheUrl() throws IOException, ServletException {
    when(request.getRequestURI()).thenReturn(URL + "//////////");

    filter.doFilter(request, response, filterChain);

    verify(digitalSignatureService).checkSignature(deleteData, securityContext);
  }

  @Test
  void tooLongUuid() {
    when(request.getRequestURI()).thenReturn(URL + "1234567890");

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> filter.doFilter(request, response, filterChain));
  }

  @Test
  void invalidUuidFormat() {
    when(request.getRequestURI()).thenReturn("/some/service/1234567890");

    Assertions.assertThrows(IllegalArgumentException.class,
        () -> filter.doFilter(request, response, filterChain));
  }

  @Test
  void shouldSetBothChecksumsToSecurityContext() throws ServletException, IOException {
    when(request.getMethod()).thenReturn("POST");
    when(request.getHeader(X_DIGITAL_SIGNATURE_DERIVED.getHeaderName()))
        .thenReturn(X_DIGITAL_SIGNATURE_DERIVED_VALUE);
    when(digitalSignatureService.copySignature(X_DIGITAL_SIGNATURE_VALUE)).thenReturn(
        SIGNATURE_CEPH_OBJECT);
    when(digitalSignatureService.copySignature(X_DIGITAL_SIGNATURE_DERIVED_VALUE)).thenReturn(
        SIGNATURE_DERIVED_CEPH_OBJECT);

    filter.doFilter(request, response, filterChain);

    verify(digitalSignatureService).checkSignature(eq(REQUEST_BODY), securityContextCaptor.capture());

    String resultSignatureChecksum = securityContextCaptor.getValue().getDigitalSignatureChecksum();
    String resultDerivedSignatureChecksum = securityContextCaptor.getValue().getDigitalSignatureDerivedChecksum();

    assertEquals(sha256Hex(SIGNATURE_CEPH_OBJECT), resultSignatureChecksum);
    assertEquals(sha256Hex(SIGNATURE_DERIVED_CEPH_OBJECT), resultDerivedSignatureChecksum);
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH"})
  void skipWhenDisabled(String arg) throws ServletException, IOException {
    filter =
        new DigitalSignatureValidationFilter(
            webConfigProperties, digitalSignatureService, mapper, false);
    when(request.getMethod()).thenReturn(arg);

    filter.doFilter(request, response, filterChain);

    verifyNoInteractions(digitalSignatureService);
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH"})
  void sameRequestWhenDisabled(String arg) throws ServletException, IOException {
    filter =
        new DigitalSignatureValidationFilter(
            webConfigProperties, digitalSignatureService, mapper, false);
    when(request.getMethod()).thenReturn(arg);

    filter.doFilter(request, response, filterChain);

    verify(filterChain).doFilter(requestCaptor.capture(), eq(response));
    assertNotEquals(MultiReadHttpServletRequest.class, requestCaptor.getValue().getClass());
  }

  @ParameterizedTest
  @ValueSource(strings = {"POST", "PUT", "PATCH"})
  void setScEvenWhenDisabled(String arg) throws ServletException, IOException {
    filter =
        new DigitalSignatureValidationFilter(
            webConfigProperties, digitalSignatureService, mapper, false);
    when(request.getMethod()).thenReturn(arg);

    filter.doFilter(request, response, filterChain);

    verify(request).setAttribute(eq(SecurityContext.class.getSimpleName()), securityContextCaptor.capture());

    String resultSignature = securityContextCaptor.getValue().getDigitalSignature();
    String resultDerivedSignature = securityContextCaptor.getValue().getDigitalSignatureDerived();

    assertEquals(X_DIGITAL_SIGNATURE_VALUE, resultSignature);
    assertEquals(X_DIGITAL_SIGNATURE_DERIVED_VALUE, resultDerivedSignature);
  }

  @Test
  void skipIfUrlIsExcludedFromFiltering() throws ServletException, IOException {
    webConfigProperties.getFilters().setExclude(List.of("/some/**"));
    filter =
            new DigitalSignatureValidationFilter(
                    webConfigProperties, digitalSignatureService, mapper, true);

    filter.doFilter(request, response, filterChain);

    verify(request, never()).setAttribute(any(), any());
    verifyNoInteractions(digitalSignatureService);
  }
}
