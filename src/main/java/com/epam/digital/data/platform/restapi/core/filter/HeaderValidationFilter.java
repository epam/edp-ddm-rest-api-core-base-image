package com.epam.digital.data.platform.restapi.core.filter;

import static com.epam.digital.data.platform.restapi.core.utils.Header.X_ACCESS_TOKEN;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_DIGITAL_SIGNATURE;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_APPLICATION;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_ACTIVITY_INSTANCE_ID;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_PROCESS;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID;
import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_SYSTEM;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.epam.digital.data.platform.restapi.core.exception.MandatoryAccessTokenClaimMissingException;
import com.epam.digital.data.platform.restapi.core.exception.MandatoryHeaderMissingException;
import com.epam.digital.data.platform.restapi.core.filter.validation.HeaderValidator;
import com.epam.digital.data.platform.restapi.core.filter.validation.UuidFormatHeaderValidator;
import com.epam.digital.data.platform.restapi.core.utils.Header;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(FiltersOrder.headerValidationFilter)
public class HeaderValidationFilter extends AbstractFilter {

  private static final Map<Header, HeaderValidator> HEADER_VALIDATOR = Map.of(
      X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID, new UuidFormatHeaderValidator(),
      X_SOURCE_BUSINESS_ACTIVITY_INSTANCE_ID, new UuidFormatHeaderValidator()
  );

  private static final List<String> MODIFYING_HTTP_METHODS = List.of(
      "POST",
      "PUT",
      "DELETE"
  );

  private static final List<Header> MODIFYING_MANDATORY_HEADERS = List.of(
      X_DIGITAL_SIGNATURE,
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
    validateHeaderFormat(request);

    filterChain.doFilter(request, response);
  }

  private void validateHeaderFormat(HttpServletRequest request) {
    HEADER_VALIDATOR.forEach((key, validator) -> validate(request, key, validator));
  }

  private void validate(HttpServletRequest request, Header header, HeaderValidator validator) {
    Optional
        .ofNullable(request.getHeader(header.getHeaderName()))
        .ifPresent(x -> validator.validate(header, x));
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
