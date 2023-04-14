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
import com.epam.digital.data.platform.dso.api.dto.VerificationRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerificationResponseDto;
import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import com.epam.digital.data.platform.dso.client.exception.BadRequestException;
import com.epam.digital.data.platform.dso.client.exception.InternalServerErrorException;
import com.epam.digital.data.platform.dso.client.exception.InvalidSignatureException;
import com.epam.digital.data.platform.integration.ceph.exception.CephCommunicationException;
import com.epam.digital.data.platform.integration.ceph.exception.MisconfigurationException;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.exception.DigitalSignatureNotFoundException;
import com.epam.digital.data.platform.restapi.core.exception.KepServiceBadRequestException;
import com.epam.digital.data.platform.restapi.core.exception.KepServiceInternalServerErrorException;
import com.epam.digital.data.platform.storage.form.dto.FormDataDto;
import com.epam.digital.data.platform.storage.form.service.FormDataStorageService;
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
  private static final FormDataDto RESPONSE_FROM_CEPH = FormDataDto.builder()
      .signature("signature")
      .build();
  private static final FormDataDto INVALID_RESPONSE_FROM_CEPH = FormDataDto.builder()
      .signature("invalid_signature")
      .build();
  private static final String INVALID_SIGNATURE = "invalid_signature";
  private static final String SIGNATURE = "signature";
  private static final String DATA = "test_data";
  private static final String CEPH_OBJECT = "{\"data\":null,\"signature\":\"signature\"}";

  private DigitalSignatureService digitalSignatureService;

  @Mock
  private FormDataStorageService lowcodeCephService;
  @Mock
  private FormDataStorageService datafactoryCephService;
  @Mock
  private CephService cephService;
  @Mock
  private DigitalSealRestClient digitalSealRestClient;

  private SecurityContext securityContext;
  private ErrorDto errorDto = ErrorDto.builder().message("DRFO mismatch").build();

  @BeforeEach
  void init() {
    MockitoAnnotations.initMocks(this);

    digitalSignatureService = new DigitalSignatureService(lowcodeCephService,
        datafactoryCephService, cephService, DATAFACTORY_BUCKET,
        digitalSealRestClient, OBJECT_MAPPER);

    securityContext = new SecurityContext(null, X_DIG_SIG, X_DIG_SIG_DERIVED);

    when(digitalSealRestClient.verify(any())).
        thenReturn(new VerificationResponseDto(true, null));
  }

  @Test
  void validSignatureTest() throws JsonProcessingException {
    when(lowcodeCephService.getFormData(X_DIG_SIG_DERIVED)).thenReturn(Optional.of(RESPONSE_FROM_CEPH));

    digitalSignatureService.checkSignature(DATA, securityContext);

    ArgumentCaptor<VerificationRequestDto> requestCaptor = ArgumentCaptor
        .forClass(VerificationRequestDto.class);
    verify(digitalSealRestClient).verify(requestCaptor.capture());

    assertEquals(SIGNATURE, requestCaptor.getValue().getSignature());
    assertEquals(DATA, requestCaptor.getValue().getData());
  }

  @Test
  void shouldThrowExceptionWithCorrectMessage() {
    when(lowcodeCephService.getFormData(X_DIG_SIG_DERIVED))
        .thenReturn(Optional.of(INVALID_RESPONSE_FROM_CEPH));
    when(digitalSealRestClient.verify(any())).
        thenReturn(new VerificationResponseDto(false, errorDto));

    String exceptionMessage = null;
    try {
      digitalSignatureService.checkSignature(DATA, securityContext);
    } catch (Exception e) {
      exceptionMessage = e.getMessage();
    }

    ArgumentCaptor<VerificationRequestDto> requestCaptor = ArgumentCaptor
        .forClass(VerificationRequestDto.class);
    verify(digitalSealRestClient).verify(requestCaptor.capture());

    assertEquals("DRFO mismatch", exceptionMessage);
    assertEquals(INVALID_SIGNATURE, requestCaptor.getValue().getSignature());
    assertEquals(DATA, requestCaptor.getValue().getData());
  }

  @Test
  void cephServiceThrowsExceptionTest() {
    when(lowcodeCephService.getFormData(X_DIG_SIG_DERIVED))
        .thenThrow(new CephCommunicationException("", new RuntimeException()));

    assertThrows(CephCommunicationException.class,
        () -> digitalSignatureService.checkSignature(DATA, securityContext));
  }

  @Test
  void cephServiceNotFoundSignature() {
    when(lowcodeCephService.getFormData(X_DIG_SIG)).thenReturn(Optional.empty());
    assertThrows(DigitalSignatureNotFoundException.class,
            () -> digitalSignatureService.checkSignature(DATA, securityContext));

    when(lowcodeCephService.getFormData(X_DIG_SIG_DERIVED)).thenReturn(Optional.empty());
    assertThrows(DigitalSignatureNotFoundException.class,
        () -> digitalSignatureService.checkSignature(DATA, securityContext));
  }

  @Test
  void misconfigurationExceptionChangedToCephBucketNotFoundException() {
    when(lowcodeCephService.getFormData(X_DIG_SIG_DERIVED))
        .thenThrow(new MisconfigurationException("Bucket A not found"));

    assertThrows(MisconfigurationException.class,
        () -> digitalSignatureService.checkSignature(DATA, securityContext));
  }

  @Test
  void badRequestExceptionChangedToKepServiceBadRequestException() {
    when(lowcodeCephService.getFormData(X_DIG_SIG_DERIVED)).thenReturn(Optional.of(RESPONSE_FROM_CEPH));
    when(digitalSealRestClient.verify(any())).thenThrow(BadRequestException.class);

    assertThrows(KepServiceBadRequestException.class, () -> digitalSignatureService
        .checkSignature(DATA, securityContext));
  }

  @Test
  void internalServerErrorExceptionChangedToKepServiceInternalServerErrorException() {
    when(lowcodeCephService.getFormData(X_DIG_SIG_DERIVED)).thenReturn(Optional.of(RESPONSE_FROM_CEPH));
    when(digitalSealRestClient.verify(any())).thenThrow(InternalServerErrorException.class);

    assertThrows(KepServiceInternalServerErrorException.class, () -> digitalSignatureService
        .checkSignature(DATA, securityContext));
  }

  @Test
  void invalidSignatureExceptionFromClientIsThrownFromService() {
    when(lowcodeCephService.getFormData(X_DIG_SIG_DERIVED)).thenReturn(Optional.of(RESPONSE_FROM_CEPH));
    when(digitalSealRestClient.verify(any())).thenThrow(InvalidSignatureException.class);

    assertThrows(InvalidSignatureException.class, () -> digitalSignatureService
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

    verify(cephService).put(eq(DATAFACTORY_BUCKET), any(), eq("value"));
  }

  @Test
  void shouldCallMethodsWithAppropriateParameters() {
    when(lowcodeCephService.getFormData(X_DIG_SIG)).thenReturn(Optional.of(RESPONSE_FROM_CEPH));

    String result = digitalSignatureService.copySignature(X_DIG_SIG);

    verify(lowcodeCephService).getFormData(X_DIG_SIG);
    verify(datafactoryCephService).putFormData(X_DIG_SIG, RESPONSE_FROM_CEPH);
    assertEquals(CEPH_OBJECT, result);
  }
}
