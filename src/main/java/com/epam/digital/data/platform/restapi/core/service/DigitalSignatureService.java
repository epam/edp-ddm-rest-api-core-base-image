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

import com.epam.digital.data.platform.dso.api.dto.SignRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerificationRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerificationResponseDto;
import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import com.epam.digital.data.platform.dso.client.exception.BadRequestException;
import com.epam.digital.data.platform.dso.client.exception.InternalServerErrorException;
import com.epam.digital.data.platform.dso.client.exception.InvalidSignatureException;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.exception.DigitalSignatureNotFoundException;
import com.epam.digital.data.platform.restapi.core.exception.KepServiceBadRequestException;
import com.epam.digital.data.platform.restapi.core.exception.KepServiceInternalServerErrorException;
import com.epam.digital.data.platform.restapi.core.utils.Header;
import com.epam.digital.data.platform.storage.form.dto.FormDataDto;
import com.epam.digital.data.platform.storage.form.service.FormDataStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DigitalSignatureService {

  private final Logger log = LoggerFactory.getLogger(DigitalSignatureService.class);

  private static final String PREFIX = "datafactory-";
  private final FormDataStorageService lowcodeFormDataStorageService;
  private final FormDataStorageService datafactoryFormDataStorageService;
  private final CephService datafactoryCephService;
  private final String datafactoryBucket;
  private final DigitalSealRestClient digitalSealRestClient;
  private final ObjectMapper objectMapper;

  public DigitalSignatureService(
      FormDataStorageService lowcodeFormDataStorageService,
      FormDataStorageService datafactoryFormDataStorageService,
      CephService datafactoryCephService,
      @Value("${datafactoryceph.bucket}") String datafactoryBucket,
      DigitalSealRestClient digitalSealRestClient,
      ObjectMapper objectMapper) {
    this.lowcodeFormDataStorageService = lowcodeFormDataStorageService;
    this.datafactoryFormDataStorageService = datafactoryFormDataStorageService;
    this.datafactoryCephService = datafactoryCephService;
    this.datafactoryBucket = datafactoryBucket;
    this.digitalSealRestClient = digitalSealRestClient;
    this.objectMapper = objectMapper;
  }
  
  public void checkSignature(String data, SecurityContext sc) {
    String signature = getSignature(sc);
    verify(signature, data);
  }

  private String getSignature(SecurityContext sc) {
    log.info("Reading Signature from lowcode storage with key {}", sc.getDigitalSignatureDerived());
    var formData = lowcodeFormDataStorageService.getFormData(sc.getDigitalSignatureDerived())
        .orElseThrow(
            () -> new DigitalSignatureNotFoundException("Signature does not exist in lowcode storage"));
    return formData.getSignature();
  }

  private void verify(String signature, String data) {
    try {
      log.info("Verifying {}", Header.X_DIGITAL_SIGNATURE_DERIVED.getHeaderName());
      VerificationResponseDto responseDto = digitalSealRestClient.verify(new VerificationRequestDto(signature, data));

      if (!responseDto.isValid()) {
        throw new InvalidSignatureException(responseDto.getError().getMessage());
      }
    } catch (BadRequestException e) {
      throw new KepServiceBadRequestException(e.getMessage());
    } catch (InternalServerErrorException e) {
      throw new KepServiceInternalServerErrorException(e.getMessage());
    }
  }

  public String copySignature(String key) {
    log.info("Copy Signature from lowcode to data ceph bucket");
    var formData = lowcodeFormDataStorageService.getFormData(key).orElseThrow(
        () -> new DigitalSignatureNotFoundException("Signature does not exist in lowcode storage"));
    datafactoryFormDataStorageService.putFormData(key, formData);
    return serialize(formData);
  }

  public <I> String sign(I input) {
    var signRequestDto = new SignRequestDto();
    try {
      signRequestDto.setData(objectMapper.writeValueAsString(input));
    } catch (JsonProcessingException e) {
      throw new RuntimeJsonMappingException(e.getMessage());
    }

    log.info("Signing content");
    return digitalSealRestClient.sign(signRequestDto).getSignature();
  }

  public String store(String value) {
    log.info("Storing object to Ceph");

    String key = PREFIX + UUID.randomUUID();
    log.debug("Generated key: {}", key);

    datafactoryCephService.put(datafactoryBucket, key, value);
    return key;
  }

  private String serialize(FormDataDto formDataDto) {
    try {
      return objectMapper.writeValueAsString(formDataDto);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Couldn't serialize object", e);
    }
  }
}
