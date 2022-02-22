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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.integration.idm.model.PublishedIdmRealm;
import com.epam.digital.data.platform.integration.idm.service.PublicIdmService;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.config.KeycloakConfigProperties;
import com.epam.digital.data.platform.restapi.core.exception.JwtExpiredException;
import com.epam.digital.data.platform.restapi.core.exception.JwtValidationException;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class JwtValidationServiceTest {

  private static final String REALM = "realm";

  @MockBean
  private KeycloakConfigProperties keycloakConfigProperties;
  @MockBean
  private PublicIdmService publicIdmService;
  @MockBean
  private Clock clock;

  private KeyPair jwtSigningKeyPair;

  private JwtValidationService jwtValidationService;

  @BeforeEach
  void beforeEach() throws NoSuchAlgorithmException {
    jwtSigningKeyPair = generateSigningKeyPair();

    jwtValidationService = new JwtValidationService(true, keycloakConfigProperties,
        publicIdmService, clock);

    when(keycloakConfigProperties.getRealms()).thenReturn(Collections.singletonList(REALM));
    when(clock.millis())
        .thenReturn(LocalDateTime.of(2021, 3, 1, 11, 50)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
    var keycloakRealmRepresentation = PublishedIdmRealm.builder()
        .publicKey(jwtSigningKeyPair.getPublic()).build();
    when(publicIdmService.getRealm(REALM))
        .thenReturn(keycloakRealmRepresentation);
  }

  @Test
  void expectOperationTokenVerifiedWhenProcessingDisabled() throws JOSEException {
    jwtValidationService = new JwtValidationService(false, keycloakConfigProperties,
        publicIdmService, clock);
    jwtValidationService.postConstruct();
    Request<Void> input = mockRequest("", new Date());

    boolean actual = jwtValidationService.isValid(input);

    assertThat(actual).isTrue();
  }

  @Test
  void expectExceptionWhenOperationWithNoToken() {
    jwtValidationService = new JwtValidationService(true, keycloakConfigProperties,
        publicIdmService, clock);
    jwtValidationService.postConstruct();
    Request<Void> input = new Request<>();

    JwtValidationException e =
        assertThrows(
            JwtValidationException.class, () -> jwtValidationService.isValid(input));
    assertThat(e.getKafkaResponseStatus()).isEqualTo(Status.JWT_INVALID);
    assertThat(e.getDetails()).isNull();
  }

  @Test
  void expectTokenNonVerifiedWhenInvalidPublicKeyReturned() throws JOSEException {
    when(publicIdmService.getRealm(REALM))
        .thenReturn(PublishedIdmRealm.builder().build());
    Date tokenExp = Date.from(LocalDateTime.of(2021, 3, 1, 12, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    Request<Void> input = mockRequest("/" + REALM, tokenExp);
    jwtValidationService.postConstruct();

    boolean actual = jwtValidationService.isValid(input);

    assertThat(actual).isFalse();
  }

  @Test
  void expectTokenIsValidWhenActualPublicKeyReturned()
      throws JOSEException {
    Date tokenExp =
        Date.from(LocalDateTime.of(2021, 3, 1, 12, 0).atZone(ZoneId.systemDefault()).toInstant());
    Request<Void> input = mockRequest("/" + REALM, tokenExp);
    jwtValidationService.postConstruct();

    var actual = jwtValidationService.isValid(input);

    assertThat(actual).isTrue();
  }

  @Test
  void expectJwtVerificationExceptionWhenIssuerRealmIncorrect() throws JOSEException {

    Date tokenExp = Date.from(LocalDateTime.of(2021, 3, 1, 12, 0)
        .atZone(ZoneId.systemDefault()).toInstant());
    Request<Void> input = mockRequest("/wrongRealm", tokenExp);
    jwtValidationService.postConstruct();

    JwtValidationException e = assertThrows(JwtValidationException.class,
        () -> jwtValidationService.isValid(input));

    assertThat(e.getKafkaResponseStatus()).isEqualTo(Status.JWT_INVALID);
  }

  @Test
  void expectTokenExpiredWhenExpDateAfterCurrent() throws JOSEException {
    Date tokenExp = Date.from(LocalDateTime.of(2021, 3, 1, 11, 45)
        .atZone(ZoneId.systemDefault()).toInstant());
    Request<Void> input = mockRequest("", tokenExp);
    jwtValidationService.postConstruct();

    JwtExpiredException e = assertThrows(JwtExpiredException.class,
        () -> jwtValidationService.isValid(input));
    assertThat(e.getKafkaResponseStatus()).isEqualTo(Status.JWT_EXPIRED);
    assertThat(e.getDetails()).isNull();
  }

  private Request<Void> mockRequest(String jwtIssuer, Date jwtExpirationTime) throws JOSEException {
    var request = new Request<Void>();
    var securityContext = new SecurityContext();
    securityContext.setAccessToken(mockJwt(jwtIssuer, jwtExpirationTime));
    request.setSecurityContext(securityContext);
    return request;
  }

  private String mockJwt(String issuer, Date expirationTime) throws JOSEException {
    JWSHeader header =
        new JWSHeader.Builder(JWSAlgorithm.RS512).type(JOSEObjectType.JWT).keyID("123").build();
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder().expirationTime(expirationTime).issuer(issuer).build();
    SignedJWT signedJWT = new SignedJWT(header, claims);
    signedJWT.sign(new RSASSASigner(jwtSigningKeyPair.getPrivate()));
    return signedJWT.serialize();
  }

  private KeyPair generateSigningKeyPair() throws NoSuchAlgorithmException {
    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(2048);
    return keyPairGenerator.generateKeyPair();
  }
}
