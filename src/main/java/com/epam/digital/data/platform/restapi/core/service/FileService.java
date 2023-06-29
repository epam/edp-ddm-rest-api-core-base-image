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
import com.epam.digital.data.platform.restapi.core.config.FileProcessing;
import com.epam.digital.data.platform.restapi.core.exception.ChecksumInconsistencyException;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import com.epam.digital.data.platform.storage.file.exception.FileNotFoundException;
import com.epam.digital.data.platform.storage.file.service.FormDataFileKeyProvider;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class FileService {

  private final Logger log = LoggerFactory.getLogger(FileService.class);

  private final FileProcessing fileProcessing;
  private final FormDataFileStorageService lowcodeFileDataStorageService;
  private final FormDataFileStorageService datafactoryFileDataStorageService;
  private final FormDataFileKeyProvider fileKeyProvider;

  public FileService(
      @Qualifier("lowcodeFileDataStorageService") FormDataFileStorageService lowcodeFileDataStorageService,
      @Qualifier("datafactoryFileDataStorageService") FormDataFileStorageService datafactoryFileDataStorageService,
      FormDataFileKeyProvider fileKeyProvider,
      FileProcessing fileProcessing) {
    this.lowcodeFileDataStorageService = lowcodeFileDataStorageService;
    this.datafactoryFileDataStorageService = datafactoryFileDataStorageService;
    this.fileKeyProvider = fileKeyProvider;
    this.fileProcessing = fileProcessing;
  }

  public boolean store(String instanceId, File file) {
    if (fileProcessing.isEnabled()) {
      log.info("Storing file '{}' from lowcode to data ceph bucket", file.getId());

      var lowcodeId = fileKeyProvider.generateKey(instanceId, file.getId());

      FileDataDto fileDataDto;
      try {
        fileDataDto = lowcodeFileDataStorageService.loadByKey(lowcodeId);
      } catch (FileNotFoundException ex) {
        log.warn("File not found ", ex);
        return false;
      }

      var content = getContent(fileDataDto.getContent());
      var calculatedChecksum = DigestUtils.sha256Hex(content);

      if (!StringUtils.equals(calculatedChecksum, file.getChecksum())) {
        throw new ChecksumInconsistencyException(
            String.format(
                "Checksum from ceph object (%s) and from request (%s) do not match. File id: '%s'",
                calculatedChecksum, file.getChecksum(), file.getId()));
      }

      fileDataDto.setContent(new ByteArrayInputStream(content));
      datafactoryFileDataStorageService.save(file.getId(), fileDataDto);
    }

    return true;
  }

  public boolean retrieve(String instanceId, File file) {
    if (fileProcessing.isEnabled()) {
      log.info("Storing file '{}' from data to lowcode ceph bucket", file.getId());

      FileDataDto fileDataDto;
      try {
        fileDataDto = datafactoryFileDataStorageService.loadByKey(file.getId());
      } catch (FileNotFoundException ex) {
        log.warn("File not found ", ex);
        return false;
      }

      var content = getContent(fileDataDto.getContent());
      var calculatedChecksum = DigestUtils.sha256Hex(content);

      if (!StringUtils.equals(calculatedChecksum, file.getChecksum())) {
        log.error("The checksum stored in the database ({}) and calculated based on the retrieved "
                + "file object ({}) do not match. File id: '{}'",
            file.getChecksum(), calculatedChecksum, file.getId());
        return true;
      }

      fileDataDto.setContent(new ByteArrayInputStream(content));
      var lowcodeId = fileKeyProvider.generateKey(instanceId, file.getId());
      lowcodeFileDataStorageService.save(lowcodeId, fileDataDto);
    }

    return true;
  }

  private byte[] getContent(InputStream inputStream) {
    try {
      return IOUtils.toByteArray(inputStream);
    } catch (IOException e) {
      throw new IllegalArgumentException("Couldn't read returned ceph content from stream", e);
    }
  }

}
