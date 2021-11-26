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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.dso.api.dto.ErrorDto;
import com.epam.digital.data.platform.dso.api.dto.SignResponseDto;
import com.epam.digital.data.platform.dso.api.dto.VerifyRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerifyResponseDto;
import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import com.epam.digital.data.platform.dso.client.exception.BadRequestException;
import com.epam.digital.data.platform.dso.client.exception.InternalServerErrorException;
import com.epam.digital.data.platform.integration.ceph.exception.CephCommunicationException;
import com.epam.digital.data.platform.integration.ceph.exception.MisconfigurationException;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.exception.DigitalSignatureNotFoundException;
import com.epam.digital.data.platform.restapi.core.exception.KepServiceBadRequestException;
import com.epam.digital.data.platform.restapi.core.exception.KepServiceInternalServerErrorException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class DigitalSignatureServiceTest {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String LOWCODE_BUCKET = "bucket";
  private static final String DATAFACTORY_BUCKET = "bucket";
  private static final String X_DIG_SIG = "xDigitalSignatureHeader";
  private static final String X_DIG_SIG_DERIVED = "xDigitalSignatureHeaderDerived";
  private static final String RESPONSE_FROM_CEPH = "{\"signature\":\"signature\",\"data\":\"\"}";
  private static final String INVALID_RESPONSE_FROM_CEPH = "{\"signature\":\"invalid_signature\",\"data\":\"Data for invalid signature\"}";
  private static final String INVALID_SIGNATURE = "invalid_signature";
  private static final String SIGNATURE = "signature";
  private static final String DATA = "test_data";
  private static final String CEPH_OBJECT = "ceph_object";

  private DigitalSignatureService digitalSignatureService;

  @Mock
  private CephService lowcodeCephService;
  @Mock
  private CephService datafactoryCephService;
  @Mock
  private DigitalSealRestClient digitalSealRestClient;

  private SecurityContext securityContext;
  private ErrorDto errorDto = ErrorDto.builder().message("DRFO mismatch").build();

  @BeforeEach
  void init() {
    MockitoAnnotations.initMocks(this);

    digitalSignatureService = new DigitalSignatureService(lowcodeCephService,
        datafactoryCephService, LOWCODE_BUCKET, DATAFACTORY_BUCKET,
        digitalSealRestClient, OBJECT_MAPPER);

    securityContext = new SecurityContext(null, X_DIG_SIG, X_DIG_SIG_DERIVED);

    when(digitalSealRestClient.verify(any())).
        thenReturn(VerifyResponseDto.builder().isValid(true).build());
  }

  @Test
  void validSignatureTest() throws JsonProcessingException {
    when(lowcodeCephService.getContent(LOWCODE_BUCKET, X_DIG_SIG_DERIVED)).thenReturn(Optional.of(RESPONSE_FROM_CEPH));

    digitalSignatureService.checkSignature(DATA, securityContext);

    ArgumentCaptor<VerifyRequestDto> requestCaptor = ArgumentCaptor
        .forClass(VerifyRequestDto.class);
    verify(digitalSealRestClient).verify(requestCaptor.capture());

    assertEquals(SIGNATURE, requestCaptor.getValue().signature());
    assertEquals(DATA, requestCaptor.getValue().data());
  }

  @Test
  void shouldThrowExceptionWithCorrectMessage() {
    when(lowcodeCephService.getContent(LOWCODE_BUCKET, X_DIG_SIG_DERIVED))
        .thenReturn(Optional.of(INVALID_RESPONSE_FROM_CEPH));
    when(digitalSealRestClient.verify(any())).
        thenReturn(VerifyResponseDto.builder().isValid(false).error(errorDto).build());

    String exceptionMessage = null;
    try {
      digitalSignatureService.checkSignature(DATA, securityContext);
    } catch (Exception e) {
      exceptionMessage = e.getMessage();
    }

    ArgumentCaptor<VerifyRequestDto> requestCaptor = ArgumentCaptor
        .forClass(VerifyRequestDto.class);
    verify(digitalSealRestClient).verify(requestCaptor.capture());

    assertEquals("DRFO mismatch", exceptionMessage);
    assertEquals(INVALID_SIGNATURE, requestCaptor.getValue().signature());
    assertEquals(DATA, requestCaptor.getValue().data());
  }

  @Test
  void cephServiceThrowsExceptionTest() {
    when(lowcodeCephService.getContent(LOWCODE_BUCKET, X_DIG_SIG_DERIVED))
        .thenThrow(new CephCommunicationException("", new RuntimeException()));

    assertThrows(CephCommunicationException.class,
        () -> digitalSignatureService.checkSignature(DATA, securityContext));
  }

  @Test
  void cephServiceNotFoundSignature() {
    when(lowcodeCephService.getContent(LOWCODE_BUCKET, X_DIG_SIG))
            .thenReturn(Optional.empty());
    assertThrows(DigitalSignatureNotFoundException.class,
            () -> digitalSignatureService.checkSignature(DATA, securityContext));

    when(lowcodeCephService.getContent(LOWCODE_BUCKET, X_DIG_SIG_DERIVED))
        .thenReturn(Optional.empty());
    assertThrows(DigitalSignatureNotFoundException.class,
        () -> digitalSignatureService.checkSignature(DATA, securityContext));
  }

  @Test
  void misconfigurationExceptionChangedToCephBucketNotFoundException() {
    when(lowcodeCephService.getContent(LOWCODE_BUCKET, X_DIG_SIG_DERIVED))
        .thenThrow(new MisconfigurationException("Bucket A not found"));

    assertThrows(MisconfigurationException.class,
        () -> digitalSignatureService.checkSignature(DATA, securityContext));
  }

  @Test
  void badRequestExceptionChangedToKepServiceBadRequestException() {
    when(lowcodeCephService.getContent(LOWCODE_BUCKET, X_DIG_SIG_DERIVED)).thenReturn(Optional.of(RESPONSE_FROM_CEPH));
    when(digitalSealRestClient.verify(any())).thenThrow(BadRequestException.class);

    assertThrows(KepServiceBadRequestException.class, () -> digitalSignatureService
        .checkSignature(DATA, securityContext));
  }

  @Test
  void internalServerErrorExceptionChangedToKepServiceInternalServerErrorException() {
    when(lowcodeCephService.getContent(LOWCODE_BUCKET, X_DIG_SIG_DERIVED)).thenReturn(Optional.of(RESPONSE_FROM_CEPH));
    when(digitalSealRestClient.verify(any())).thenThrow(InternalServerErrorException.class);

    assertThrows(KepServiceInternalServerErrorException.class, () -> digitalSignatureService
        .checkSignature(DATA, securityContext));
  }

  @Test
  void signRequest() {
    SignResponseDto signResponseDto = new SignResponseDto();
    signResponseDto.setSignature("Signature");
    when(digitalSealRestClient.sign(any())).thenReturn(signResponseDto);

    String responseDto = digitalSignatureService.sign(new Request<>());

    assertEquals("Signature", responseDto);
  }

  @Test
  void shouldCallPutContentWithAppropriateParameters() {
    digitalSignatureService.store("value");

    verify(datafactoryCephService).putContent(eq(DATAFACTORY_BUCKET), any(), eq("value"));
  }

  @Test
  void shouldCallMethodsWithAppropriateParameters() {
    when(lowcodeCephService.getContent(LOWCODE_BUCKET, X_DIG_SIG)).thenReturn(Optional.of(CEPH_OBJECT));

    String result = digitalSignatureService.copySignature(X_DIG_SIG);

    verify(lowcodeCephService).getContent(LOWCODE_BUCKET, X_DIG_SIG);
    verify(datafactoryCephService).putContent(DATAFACTORY_BUCKET, X_DIG_SIG, CEPH_OBJECT);
    assertEquals(CEPH_OBJECT, result);
  }
}
