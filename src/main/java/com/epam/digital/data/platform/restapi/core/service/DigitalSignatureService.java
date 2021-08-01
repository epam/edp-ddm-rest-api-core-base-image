package com.epam.digital.data.platform.restapi.core.service;

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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

@Component
public class DigitalSignatureService {

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
  private final RestAuditEventsFacade restAuditEventsFacade;

  public DigitalSignatureService(
      CephService lowcodeCephService,
      CephService datafactoryCephService,
      @Value("${ceph.bucket}") String lowcodeBucket,
      @Value("${datafactoryceph.bucket}") String datafactoryBucket,
      DigitalSignatureRestClient digitalSignatureRestClient,
      DigitalSealRestClient digitalSealRestClient,
      @Value("${data-platform.signature.validation.enabled}") boolean isEnabled,
      ObjectMapper objectMapper,
      RestAuditEventsFacade restAuditEventsFacade) {
    this.lowcodeCephService = lowcodeCephService;
    this.datafactoryCephService = datafactoryCephService;
    this.lowcodeBucket = lowcodeBucket;
    this.datafactoryBucket = datafactoryBucket;
    this.digitalSignatureRestClient = digitalSignatureRestClient;
    this.digitalSealRestClient = digitalSealRestClient;
    this.isEnabled = isEnabled;
    this.objectMapper = objectMapper;
    this.restAuditEventsFacade = restAuditEventsFacade;
  }

  public void checkSignature(String data, SecurityContext sc) throws JsonProcessingException {
    if (!isEnabled) {
      return;
    }

    String key = isNotEmpty(sc.getDigitalSignatureDerived())
        ? sc.getDigitalSignatureDerived()
        : sc.getDigitalSignature();

    String signature;
    String responseFromCeph =
        lowcodeCephService
            .getContent(lowcodeBucket, key)
            .orElseThrow(
                () -> new DigitalSignatureNotFoundException("Signature does not exist in ceph bucket"));
    Map<String, Object> cephResponse = objectMapper.readValue(responseFromCeph, Map.class);
    signature = (String) cephResponse.get(SIGNATURE);

    verify(
        signature, data, StringUtils.isEmpty(sc.getDigitalSignatureDerived()), sc.getAccessToken());
  }

  public <I> String sign(I input) {
    var signRequestDto = new SignRequestDto();
    try {
      signRequestDto.setData(objectMapper.writeValueAsString(input));
    } catch (JsonProcessingException e) {
      throw new RuntimeJsonMappingException(e.getMessage());
    }
    return digitalSealRestClient.sign(signRequestDto).getSignature();
  }

  public String store(String value) {
    String key = PREFIX + UUID.randomUUID();
    datafactoryCephService.putContent(datafactoryBucket, key, value);
    return key;
  }

  public String saveSignature(String key) {
    if (!isEnabled) {
      return "";
    }
    String value =
        lowcodeCephService
            .getContent(lowcodeBucket, key)
            .orElseThrow(
                () -> new DigitalSignatureNotFoundException("Signature does not exist in ceph bucket"));
    datafactoryCephService.putContent(datafactoryBucket, key, value);
    return value;
  }

  private void verify(String signature, String data, boolean emptyDerivedHeader, String jwt) {
    try {
      VerifyResponseDto responseDto;
      if (emptyDerivedHeader) {
        responseDto = digitalSignatureRestClient.verify(new VerifyRequestDto(signature, data));
      } else {
        responseDto = digitalSealRestClient.verify(new VerifyRequestDto(signature, data));
      }
      if (!responseDto.isValid) {
        restAuditEventsFacade.auditSignatureInvalid(jwt);
        throw new InvalidSignatureException(responseDto.error.getMessage());
      }
    } catch (BadRequestException e) {
      throw new KepServiceBadRequestException(e.getMessage());
    } catch (InternalServerErrorException e) {
      throw new KepServiceInternalServerErrorException(e.getMessage());
    }
  }
}
