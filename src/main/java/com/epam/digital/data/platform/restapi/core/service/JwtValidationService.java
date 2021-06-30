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

package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.integration.idm.model.PublishedIdmRealm;
import com.epam.digital.data.platform.integration.idm.service.PublicIdmService;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.config.KeycloakConfigProperties;
import com.epam.digital.data.platform.restapi.core.exception.JwtExpiredException;
import com.epam.digital.data.platform.restapi.core.exception.JwtValidationException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import java.security.PublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.representations.JsonWebToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtValidationService {

  private final Logger log = LoggerFactory.getLogger(JwtValidationService.class);

  private final boolean jwtValidationEnabled;
  private final KeycloakConfigProperties keycloakConfigProperties;

  private final PublicIdmService publicIdmService;
  private final Clock clock;

  private Map<String, PublishedIdmRealm> allowedRealmsRepresentations;

  public JwtValidationService(
      @Value("${data-platform.jwt.validation.enabled}") boolean jwtValidationEnabled,
      KeycloakConfigProperties keycloakConfigProperties,
      PublicIdmService publicIdmService, Clock clock) {
    this.jwtValidationEnabled = jwtValidationEnabled;
    this.keycloakConfigProperties = keycloakConfigProperties;
    this.publicIdmService = publicIdmService;
    this.clock = clock;
  }

  @PostConstruct
  void postConstruct() {
    if (jwtValidationEnabled) {
      allowedRealmsRepresentations =
          keycloakConfigProperties.getRealms().stream()
              .collect(
                  Collectors.toMap(
                      Function.identity(), publicIdmService::getRealm));
    }
  }

  public <O> boolean isValid(Request<O> input) {
    if (!jwtValidationEnabled) {
      return true;
    }

    String accessToken = getTokenFromInput(input);
    JWTClaimsSet jwtClaimsSet = getClaimsFromToken(accessToken);
    if (isExpiredJwt(jwtClaimsSet)) {
      throw new JwtExpiredException("JWT is expired");
    }
    String jwtIssuer = jwtClaimsSet.getIssuer();
    String issuerRealm = jwtIssuer.substring(jwtIssuer.lastIndexOf("/") + 1);

    if (keycloakConfigProperties.getRealms().contains(issuerRealm)) {
      PublicKey keycloakPublicKey = allowedRealmsRepresentations.get(issuerRealm).getPublicKey();
      return isVerifiedToken(accessToken, keycloakPublicKey);
    } else {
      throw new JwtValidationException("Issuer realm is not valid");
    }
  }

  private JWTClaimsSet getClaimsFromToken(String accessToken) {
    try {
      return JWTParser.parse(accessToken)
          .getJWTClaimsSet();
    } catch (ParseException e) {
      throw new JwtValidationException("Error while JWT parsing", e);
    }
  }

  private <O> String getTokenFromInput(Request<O> input) {
    return Optional.ofNullable(input.getSecurityContext())
        .map(SecurityContext::getAccessToken)
        .orElse("");
  }

  private boolean isExpiredJwt(JWTClaimsSet jwtClaimsSet) {
    Date now = new Date(clock.millis());
    return Optional.of(jwtClaimsSet.getExpirationTime())
        .map(now::after)
        .orElse(true);
  }

  private boolean isVerifiedToken(String accessToken, PublicKey publicKey) {
    try {
      TokenVerifier.create(accessToken, JsonWebToken.class)
          .publicKey(publicKey)
          .verify();
      return true;
    } catch (VerificationException e) {
      log.error("JWT token is not valid", e);
      return false;
    }
  }
}
