/*
 * Copyright 2023 EPAM Systems.
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

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.restapi.core.config.CsvConfig;
import com.epam.digital.data.platform.restapi.core.exception.ChecksumInconsistencyException;
import com.epam.digital.data.platform.restapi.core.exception.CsvFileEncodingException;
import com.epam.digital.data.platform.restapi.core.exception.CsvFileParsingException;
import com.epam.digital.data.platform.restapi.core.exception.CsvDtoValidationException;
import com.epam.digital.data.platform.restapi.core.service.impl.CsvProcessorTestImpl;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import com.epam.digital.data.platform.storage.file.service.FormDataFileKeyProvider;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import com.fasterxml.jackson.dataformat.csv.CsvReadException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.io.InputStream;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import lombok.SneakyThrows;

@SpringBootTest(
    classes = {CsvConfig.class, LocalValidatorFactoryBean.class, CsvProcessorTestImpl.class})
class CsvProcessorTest {

  private static final String FILE_ID = "1";

  private static final String BP_INSTANCE_ID = "2";
  private static final String LOWCODE_FILE_ID = "2/1";

  @MockBean
  private FormDataFileStorageService lowcodeCephService;
  @MockBean
  private FormDataFileKeyProvider fileKeyProvider;

  @Autowired
  private CsvProcessorTestImpl instance;

  @Test
  void expectValidFileProcessedToPayload() {
    when(fileKeyProvider.generateKey(BP_INSTANCE_ID, FILE_ID)).thenReturn(LOWCODE_FILE_ID);
    String filename = "/csv/mockEntity.csv";
    when(lowcodeCephService.loadByKey(LOWCODE_FILE_ID))
        .thenReturn(FileDataDto.builder().content(getFileContent(filename)).build());

    var fileChecksum = getFileChecksum(filename);

    var actualPayload = instance.transformFileToEntity(mockRequest(fileChecksum));

    assertThat(actualPayload.getEntities()).hasSize(2);

    assertThat(actualPayload.getEntities()[0].getConsentDate())
        .isEqualTo(LocalDateTime.of(2021, 1, 29, 11, 8, 16, 631000000));
    assertThat(actualPayload.getEntities()[0].getPersonFullName()).isEqualTo("Name");
    assertThat(actualPayload.getEntities()[0].getPersonPassNumber()).isEqualTo("АА111132");
    assertThat(actualPayload.getEntities()[0].getPassportScanCopy()).isNull();

    assertThat(actualPayload.getEntities()[1].getConsentDate())
        .isEqualTo(LocalDateTime.of(2021, 1, 30, 11, 8, 16, 631000000));
    assertThat(actualPayload.getEntities()[1].getPersonFullName()).isEqualTo("Name2");
    assertThat(actualPayload.getEntities()[1].getPersonPassNumber()).isEqualTo("АА111133");
    assertThat(actualPayload.getEntities()[1].getPassportScanCopy()).isNull();
  }

  @Test
  void expectExceptionOnInvalidChecksum() {
    when(fileKeyProvider.generateKey(BP_INSTANCE_ID, FILE_ID)).thenReturn(LOWCODE_FILE_ID);
    when(lowcodeCephService.loadByKey(LOWCODE_FILE_ID))
        .thenReturn(FileDataDto.builder().content(getFileContent("/csv/mockEntity.csv")).build());

    var request = mockRequest("Invalid checksum");

    assertThrows(ChecksumInconsistencyException.class,
            () -> instance.transformFileToEntity(request));
  }

  @Test
  void expectExceptionOnInvalidEncoding() {
    when(fileKeyProvider.generateKey(BP_INSTANCE_ID, FILE_ID)).thenReturn(LOWCODE_FILE_ID);
    String filename = "/csv/mockEntityInvalidEncoding.csv";
    when(lowcodeCephService.loadByKey(LOWCODE_FILE_ID))
        .thenReturn(FileDataDto.builder().content(getFileContent(filename)).build());

    var fileChecksum = getFileChecksum(filename);
    var request = mockRequest(fileChecksum);

    assertThrows(
        CsvFileEncodingException.class, () -> instance.transformFileToEntity(request));
  }

  @Test
  void expectExceptionOnInvalidCsvContent() {
    when(fileKeyProvider.generateKey(BP_INSTANCE_ID, FILE_ID)).thenReturn(LOWCODE_FILE_ID);
    String filename = "/csv/mockEntityInvalidCsvFormat.csv";
    when(lowcodeCephService.loadByKey(LOWCODE_FILE_ID))
        .thenReturn(FileDataDto.builder().content(getFileContent(filename)).build());

    var fileChecksum = getFileChecksum(filename);
    var request = mockRequest(fileChecksum);

    var actualException =
        assertThrows(CsvFileParsingException.class,
            () -> instance.transformFileToEntity(request));
    assertThat(actualException.getCause()).isExactlyInstanceOf(CsvReadException.class);
  }

  @Test
  void expectValidationExceptionOnCsvContentDto() {
    when(fileKeyProvider.generateKey(BP_INSTANCE_ID, FILE_ID)).thenReturn(LOWCODE_FILE_ID);
    String filename = "/csv/mockEntityInvalidPassFormat.csv";
    when(lowcodeCephService.loadByKey(LOWCODE_FILE_ID))
        .thenReturn(FileDataDto.builder().content(getFileContent(filename)).build());

    var fileChecksum = getFileChecksum(filename);
    var request = mockRequest(fileChecksum);

    var actualException =
        assertThrows(CsvDtoValidationException.class,
            () -> instance.transformFileToEntity(request));
    assertThat(actualException.getBindingResult().getErrorCount()).isEqualTo(1);
    assertThat(actualException.getBindingResult().getFieldErrors().get(0).getField())
        .isEqualTo("entities[0].personPassNumber");
  }

  private Request<File> mockRequest(String fileChecksum) {
    var file = new File(FILE_ID, fileChecksum);
    var requestContext = new RequestContext();
    requestContext.setBusinessProcessInstanceId(BP_INSTANCE_ID);
    return new Request<>(file, requestContext, null);
  }

  private InputStream getFileContent(String name) {
    return CsvProcessorTest.class.getResourceAsStream(name);
  }

  @SneakyThrows
  private String getFileChecksum(String filename) {
    return DigestUtils.sha256Hex(IOUtils.toByteArray(getFileContent(filename)));
  }
}
