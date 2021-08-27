package com.epam.digital.data.platform.restapi.core.service;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import com.epam.digital.data.platform.dso.api.dto.SignRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerifyRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerifyResponseDto;
import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import com.epam.digital.data.platform.dso.client.DigitalSignatureRestClient;
import com.epam.digital.data.platform.dso.client.exception.BadRequestException;
import com.epam.digital.data.platform.dso.client.exception.InternalServerErrorException;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.exception.DigitalSignatureNotFoundException;
import com.epam.digital.data.platform.restapi.core.exception.InvalidSignatureException;
import com.epam.digital.data.platform.restapi.core.exception.KepServiceBadRequestException;
import com.epam.digital.data.platform.restapi.core.exception.KepServiceInternalServerErrorException;
import com.epam.digital.data.platform.restapi.core.utils.Header;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DigitalSignatureService {

  private final Logger log = LoggerFactory.getLogger(DigitalSignatureService.class);

  private static final String SIGNATURE = "signature";
  private static final String PREFIX = "datafactory-";
  private final CephService lowcodeCephService;
  private final CephService datafactoryCephService;
  private final String lowcodeBucket;
  private final String datafactoryBucket;
  private final DigitalSignatureRestClient digitalSignatureRestClient;
  private final DigitalSealRestClient digitalSealRestClient;
  private final boolean isEnabled;
  private final ObjectMapper objectMapper;

  public DigitalSignatureService(
      CephService lowcodeCephService,
      CephService datafactoryCephService,
      @Value("${ceph.bucket}") String lowcodeBucket,
      @Value("${datafactoryceph.bucket}") String datafactoryBucket,
      DigitalSignatureRestClient digitalSignatureRestClient,
      DigitalSealRestClient digitalSealRestClient,
      @Value("${data-platform.signature.validation.enabled}") boolean isEnabled,
      ObjectMapper objectMapper) {
    this.lowcodeCephService = lowcodeCephService;
    this.datafactoryCephService = datafactoryCephService;
    this.lowcodeBucket = lowcodeBucket;
    this.datafactoryBucket = datafactoryBucket;
    this.digitalSignatureRestClient = digitalSignatureRestClient;
    this.digitalSealRestClient = digitalSealRestClient;
    this.isEnabled = isEnabled;
    this.objectMapper = objectMapper;
  }

  public void checkSignature(String data, SecurityContext sc) throws JsonProcessingException {
    if (!isEnabled) {
      return;
    }

    String key = isNotEmpty(sc.getDigitalSignatureDerived())
        ? sc.getDigitalSignatureDerived()
        : sc.getDigitalSignature();

    String signature = getSignature(key);
    verify(signature, data, StringUtils.isEmpty(sc.getDigitalSignatureDerived()));
  }

  private String getSignature(String key) throws JsonProcessingException {
    log.info("Reading Signature from Ceph");
    String responseFromCeph =
        lowcodeCephService
            .getContent(lowcodeBucket, key)
            .orElseThrow(
                () -> new DigitalSignatureNotFoundException("Signature does not exist in ceph bucket"));
    Map<String, Object> cephResponse = objectMapper.readValue(responseFromCeph, Map.class);
    return (String) cephResponse.get(SIGNATURE);
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

    datafactoryCephService.putContent(datafactoryBucket, key, value);
    return key;
  }

  public String saveSignature(String key) {
    if (!isEnabled) {
      return "";
    }

    log.info("Copy Signature from lowcode to data ceph bucket");
    String value =
        lowcodeCephService
            .getContent(lowcodeBucket, key)
            .orElseThrow(
                () -> new DigitalSignatureNotFoundException("Signature does not exist in ceph bucket"));
    datafactoryCephService.putContent(datafactoryBucket, key, value);
    return value;
  }

  private void verify(String signature, String data, boolean emptyDerivedHeader) {
    try {
      VerifyResponseDto responseDto;
      if (emptyDerivedHeader) {
        log.info("Verifying {}", Header.X_DIGITAL_SIGNATURE.getHeaderName());
        responseDto = digitalSignatureRestClient.verify(new VerifyRequestDto(signature, data));
      } else {
        log.info("Verifying {}", Header.X_DIGITAL_SIGNATURE_DERIVED.getHeaderName());
        responseDto = digitalSealRestClient.verify(new VerifyRequestDto(signature, data));
      }

      if (!responseDto.isValid) {
        throw new InvalidSignatureException(responseDto.error.getMessage());
      }
    } catch (BadRequestException e) {
      throw new KepServiceBadRequestException(e.getMessage());
    } catch (InternalServerErrorException e) {
      throw new KepServiceInternalServerErrorException(e.getMessage());
    }
  }
}
