package com.epam.digital.data.platform.restapi.core.util;

import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.starter.security.dto.RolesDto;
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
import java.util.Collections;

public final class SecurityUtils {

  private static final String ROLES_CLAIM = "realm_access";

  public static SecurityContext mockSecurityContext() throws JOSEException {
    var sc = new SecurityContext();
    sc.setAccessToken(mockJwt());
    return sc;
  }

  public static String mockJwt() throws JOSEException {
    ECKey key = new ECKeyGenerator(Curve.P_521).keyID("123").generate();
    JWSHeader header =
        new JWSHeader.Builder(JWSAlgorithm.ES512)
            .type(JOSEObjectType.JWT)
            .keyID(key.getKeyID())
            .build();
    RolesDto rolesDto = new RolesDto();
    rolesDto.setRoles(Collections.emptyList());
    JWTClaimsSet claims = new JWTClaimsSet.Builder()
            .claim(ROLES_CLAIM, rolesDto)
            .build();
    SignedJWT signedJWT = new SignedJWT(header, claims);
    signedJWT.sign(new ECDSASigner(key.toECPrivateKey()));
    return signedJWT.serialize();
  }
}
