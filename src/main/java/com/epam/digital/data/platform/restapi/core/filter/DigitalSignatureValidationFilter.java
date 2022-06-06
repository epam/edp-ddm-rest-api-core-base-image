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

import static com.epam.digital.data.platform.restapi.core.utils.Header.X_ACCESS_TOKEN;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE_DERIVED;
import static org.springframework.util.StringUtils.isEmpty;

import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.service.DigitalSignatureService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UrlPathHelper;

@Component
@Order(FiltersOrder.DIGITAL_SIGNATURE_VALIDATION_FILTER)
public class DigitalSignatureValidationFilter extends AbstractFilter {

  private static final Set<String> applicableHttpMethods = Set.of("PUT", "POST", "DELETE", "PATCH");

  private final DigitalSignatureService digitalSignatureService;
  private final ObjectMapper mapper;
  private final boolean isEnabled;

  public DigitalSignatureValidationFilter(DigitalSignatureService digitalSignatureService,
      ObjectMapper mapper, @Value("${data-platform.signature.validation.enabled}") boolean isEnabled) {
    this.digitalSignatureService = digitalSignatureService;
    this.mapper = mapper;
    this.isEnabled = isEnabled;
  }

  @Override
  public void doFilterInternal(HttpServletRequest request, HttpServletResponse servletResponse,
      FilterChain filterChain) throws IOException, ServletException {

    String method = request.getMethod().toUpperCase();

    SecurityContext securityContext = new SecurityContext();
    securityContext.setAccessToken(request.getHeader(X_ACCESS_TOKEN.getHeaderName()));

    if (applicableHttpMethods.contains(method)) {

      fillContextSignatures(securityContext, request);

      if (isEnabled) {
        String data;
        if (method.equals("DELETE")) {
          data = getDataForDelete(request);
        } else {
          request = new MultiReadHttpServletRequest(request);
          data = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        }

        digitalSignatureService.checkSignature(data, securityContext);
        saveSignatures(securityContext);
      }
    }

    request.setAttribute(SecurityContext.class.getSimpleName(), securityContext);
    filterChain.doFilter(request, servletResponse);
  }

  private SecurityContext saveSignatures(SecurityContext sc) {
    String signature = digitalSignatureService.copySignature(sc.getDigitalSignature());
    sc.setDigitalSignatureChecksum(DigestUtils.sha256Hex(signature));

    String signatureDerived = digitalSignatureService.copySignature(sc.getDigitalSignatureDerived());
    sc.setDigitalSignatureDerivedChecksum(DigestUtils.sha256Hex(signatureDerived));

    return sc;
  }

  private String getDataForDelete(HttpServletRequest request) {
    String fullPath = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
    String url = RegExUtils.removePattern(fullPath, "/+$");
    var id = UUID.fromString(url.substring(url.lastIndexOf('/') + 1));
    return serialize(Map.of("id", id.toString()));
  }

  private SecurityContext fillContextSignatures(SecurityContext securityContext, HttpServletRequest request) {
    String xDigitalSignature = request.getHeader(X_DIGITAL_SIGNATURE.getHeaderName());
    if (isEmpty(xDigitalSignature) && isEnabled) {
      throw new IllegalArgumentException("Missing required Header X-Digital-Signature");
    }
    securityContext.setDigitalSignature(xDigitalSignature);

    String xDigitalSignatureDerived = request.getHeader(X_DIGITAL_SIGNATURE_DERIVED.getHeaderName());
    if (isEmpty(xDigitalSignatureDerived) && isEnabled) {
      throw new IllegalArgumentException("Missing required Header X-Digital-Signature-Derived");
    }
    securityContext.setDigitalSignatureDerived(xDigitalSignatureDerived);

    return securityContext;
  }

  private String serialize(Map<String, String> data) {
    try {
      return mapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Couldn't serialize object", e);
    }
  }
}
