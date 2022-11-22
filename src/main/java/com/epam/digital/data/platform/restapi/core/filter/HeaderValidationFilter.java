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
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_APPLICATION;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_PROCESS;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_SYSTEM;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.epam.digital.data.platform.restapi.core.exception.MandatoryAccessTokenClaimMissingException;
import com.epam.digital.data.platform.restapi.core.exception.MandatoryHeaderMissingException;
import com.epam.digital.data.platform.restapi.core.utils.Header;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(FiltersOrder.HEADER_VALIDATION_FILTER)
public class HeaderValidationFilter extends AbstractFilter {

  private static final List<String> MODIFYING_HTTP_METHODS = List.of(
      "POST",
      "PUT",
      "DELETE"
  );

  private static final List<Header> MODIFYING_MANDATORY_HEADERS = List.of(
      X_DIGITAL_SIGNATURE,
      X_DIGITAL_SIGNATURE_DERIVED,
      X_SOURCE_SYSTEM,
      X_SOURCE_APPLICATION,
      X_SOURCE_BUSINESS_PROCESS
  );

  private final TokenParser tokenParser;

  public HeaderValidationFilter(TokenParser tokenParser) {
    this.tokenParser = tokenParser;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    validateModifyingMandatoryHeaders(request);
    validateAccessTokenClaims(request);

    filterChain.doFilter(request, response);
  }

  private void validateAccessTokenClaims(HttpServletRequest request) {
    JwtClaimsDto claims;
    try {
      var token = request.getHeader(X_ACCESS_TOKEN.getHeaderName());
      claims = tokenParser.parseClaims(token);
    } catch (JwtParsingException e) {
      // should never happen but still treat the case as there are no expected claims
      claims = new JwtClaimsDto();
    }

    var missed = new ArrayList<String>();

    if (isBlank(claims.getDrfo())) {
      missed.add("drfo");
    }

    if (isBlank(claims.getFullName())) {
      missed.add("fullName");
    }

    if (isNotEmpty(missed)) {
      throw new MandatoryAccessTokenClaimMissingException(missed);
    }
  }

  private void validateModifyingMandatoryHeaders(HttpServletRequest request) {
    if (MODIFYING_HTTP_METHODS.contains(request.getMethod().toUpperCase())) {
      var missed = MODIFYING_MANDATORY_HEADERS.stream()
          .map(Header::getHeaderName)
          .filter(x -> missed(x, request))
          .collect(toList());

      if (isNotEmpty(missed)) {
        throw new MandatoryHeaderMissingException(missed);
      }
    }
  }

  private boolean missed(String header, HttpServletRequest request) {
    return request.getHeader(header) == null;
  }
}
