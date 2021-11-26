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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.exception.JwtValidationException;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class JwtInfoProviderTest {

  private static final String DRFO_CLAIM_NAME = "drfo";
  private static final String DRFO_CLAIM_VALUE = "1";

  @Mock
  private TokenParser tokenParser;

  private JwtInfoProvider jwtInfoProvider;

  @BeforeEach
  void beforeEach() {
    jwtInfoProvider = new JwtInfoProvider(tokenParser);
  }

  @Test
  void expectJwtIsParsed() throws JOSEException {
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .claim(DRFO_CLAIM_NAME, DRFO_CLAIM_VALUE)
            .build();
    Request<Void> input = mockRequest(claims);
    JwtClaimsDto jwtClaimsDto = new JwtClaimsDto();
    when(tokenParser.parseClaims(any())).thenReturn(jwtClaimsDto);

    JwtClaimsDto actual = jwtInfoProvider.getUserClaims(input);

    assertThat(actual).isEqualTo(jwtClaimsDto);
    verify(tokenParser).parseClaims(input.getSecurityContext().getAccessToken());
  }

  @Test
  void expectJwtValidationExceptionIfParsingError() {
    when(tokenParser.parseClaims(any()))
            .thenThrow(new JwtParsingException(""));

    assertThrows(JwtValidationException.class, () -> jwtInfoProvider.getUserClaims(new Request<>()));
  }

  private Request<Void> mockRequest(JWTClaimsSet jwtClaims) throws JOSEException {
    var request = new Request<Void>();
    var securityContext = new SecurityContext();
    securityContext.setAccessToken(mockJwt(jwtClaims));
    request.setSecurityContext(securityContext);
    return request;
  }

  private String mockJwt(JWTClaimsSet jwtClaimsSet) throws JOSEException {
    ECKey key = new ECKeyGenerator(Curve.P_521)
        .keyID("123")
        .generate();
    JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES512)
        .type(JOSEObjectType.JWT)
        .keyID(key.getKeyID())
        .build();
    SignedJWT signedJWT = new SignedJWT(header, jwtClaimsSet);
    signedJWT.sign(new ECDSASigner(key.toECPrivateKey()));
    return signedJWT.serialize();
  }
}