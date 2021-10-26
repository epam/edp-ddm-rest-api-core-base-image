package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.exception.JwtValidationException;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class JwtInfoProvider {

  private final TokenParser tokenParser;

  public JwtInfoProvider(TokenParser tokenParser) {
    this.tokenParser = tokenParser;
  }

  public <O> JwtClaimsDto getUserClaims(Request<O> input) {
    String accessToken = getTokenFromInput(input);
    try {
      return tokenParser.parseClaims(accessToken);
    } catch (JwtParsingException e) {
      throw new JwtValidationException("Error while getting JWT claims", e);
    }
  }

  private <O> String getTokenFromInput(Request<O> input) {
    return Optional.ofNullable(input.getSecurityContext())
        .map(SecurityContext::getAccessToken)
        .orElse("");
  }
}
