/*
 * Copyright 2022 EPAM Systems.
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
import com.epam.digital.data.platform.restapi.core.exception.ChecksumInconsistencyException;
import com.epam.digital.data.platform.restapi.core.exception.CsvFileEncodingException;
import com.epam.digital.data.platform.restapi.core.exception.DtoValidationException;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import com.epam.digital.data.platform.storage.file.service.FormDataFileKeyProvider;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.parser.txt.CharsetDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Validator;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractCsvProcessor<U, V> implements CsvProcessor<V> {

  private final Logger log = LoggerFactory.getLogger(AbstractCsvProcessor.class);

  @Autowired
  private FormDataFileStorageService lowcodeFileDataStorageService;
  @Autowired
  private FormDataFileKeyProvider fileKeyProvider;
  @Autowired
  private Function<Class<?>, ObjectReader> csvReaderFactory;
  @Autowired
  private Validator validator;

  @Override
  public V transformFileToEntity(Request<File> input) {
    var file = input.getPayload();
    log.info("Getting csv file '{}' from lowcode ceph bucket", file.getId());

    var lowcodeId =
        fileKeyProvider.generateKey(
            input.getRequestContext().getBusinessProcessInstanceId(), file.getId());

    FileDataDto fileDataDto = lowcodeFileDataStorageService.loadByKey(lowcodeId);

    var content = getContent(fileDataDto.getContent());
    validateContent(content, file);

    var newRequestPayload = getPayloadObjectFromContent(content);
    validatePayloadObject(newRequestPayload);
    return newRequestPayload;
  }

  private byte[] getContent(InputStream inputStream) {
    log.info("Getting csv file content");
    try {
      return IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      throw new IllegalArgumentException("Couldn't read returned ceph content from stream", e);
    }
  }

  private void validateContent(byte[] content, File fileDto) {
    log.info("Validating csv file content");
    var fileActualChecksum = DigestUtils.sha256Hex(content);
    if (!StringUtils.equals(fileActualChecksum, fileDto.getChecksum())) {
      throw new ChecksumInconsistencyException(
          String.format(
              "Checksum from ceph object (%s) and from request (%s) do not match. File id: '%s'",
              fileActualChecksum, fileDto.getChecksum(), fileDto.getId()));
    }

    CharsetDetector charsetDetector = new CharsetDetector(content.length);
    charsetDetector.setText(content);
    var encoding = charsetDetector.detectAll()[0].getName();
    if (!StringUtils.equals(encoding, StandardCharsets.UTF_8.name())) {
      throw new CsvFileEncodingException(
          "Wrong csv file encoding found instead of UTF-8: " + encoding);
    }
  }

  private V getPayloadObjectFromContent(byte[] content) {
    log.info("Processing csv file content");
    var reader = csvReaderFactory.apply(getCsvRowElementType());
    try (MappingIterator<U> csvRowsContent = reader.readValues(content)) {
      var csvRowsContentList = csvRowsContent.readAll();
      return getPayloadObjectFromCsvRows(csvRowsContentList);
    } catch (IOException exception) {
      throw new IllegalArgumentException("Exception on processing csv file content", exception);
    }
  }

  private void validatePayloadObject(V payloadObject) {
    log.info("Validating dto retrieved from csv file content");
    var errors = new BeanPropertyBindingResult(payloadObject, payloadObject.getClass().getName());
    validator.validate(payloadObject, errors);
    if (errors.hasErrors()) {
      throw new DtoValidationException("Failed validation of csv file content", errors);
    }
  }

  protected abstract Class<U> getCsvRowElementType();

  protected abstract V getPayloadObjectFromCsvRows(List<U> rows);
}
