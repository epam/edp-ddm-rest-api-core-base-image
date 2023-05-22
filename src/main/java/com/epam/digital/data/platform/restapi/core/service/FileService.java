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

import static java.util.Collections.singletonList;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.restapi.core.config.FileProcessing;
import com.epam.digital.data.platform.restapi.core.exception.ChecksumInconsistencyException;
import com.epam.digital.data.platform.restapi.core.model.FileProperty;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import com.epam.digital.data.platform.storage.file.exception.FileNotFoundException;
import com.epam.digital.data.platform.storage.file.service.FormDataFileKeyProvider;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.utils.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

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

  public List<FileProperty> getFileProperties(Object obj) {
    Collection<Object> objs;

    if (Collection.class.isAssignableFrom(obj.getClass())) {
      objs = (Collection<Object>) obj;
    } else {
      objs = singletonList(obj);
    }

    return getFileProperties(objs);
  }

  private List<FileProperty> getFileProperties(Collection<Object> requestBodyObjects) {
    log.debug("Request body elements: {}", requestBodyObjects.size());
    List<FileProperty> fileProperties = Lists.newArrayList();
    requestBodyObjects.forEach(object -> fillFilePropertiesFromObject(object, fileProperties));
    log.debug("Total number of files in the request: {}", fileProperties.size());
    return fileProperties;
  }

  @SuppressWarnings("unchecked")
  private void fillFilePropertiesFromObject(Object objectFromRequestBody,
      List<FileProperty> fileProperties) {
    Arrays.stream(objectFromRequestBody.getClass().getDeclaredFields())
        .forEach(fieldFromObject -> {
              ReflectionUtils.makeAccessible(fieldFromObject);
              var fieldFromObjectType = fieldFromObject.getType();
              // check if the current field is of type com.epam.digital.data.platform.model.core.kafka.File
              if (File.class.equals(fieldFromObjectType)) {
                addFilePropertyToList(fileProperties, fieldFromObject, objectFromRequestBody);
                // check if the current field represents an array class and does not consist of primitive elements
              } else if (fieldFromObjectType.isArray() && !fieldFromObjectType.getComponentType().isPrimitive()) {
                var fieldValueTypeArray = (Object[]) ReflectionUtils.getField(fieldFromObject,
                    objectFromRequestBody);
                var convertedListFromArray =
                    Objects.nonNull(fieldValueTypeArray) ? List.of(fieldValueTypeArray)
                        : Lists.newArrayList();
                addFilePropertiesToList(fileProperties, convertedListFromArray, fieldFromObject);
                // check if the current field is of type java.util.List
              } else if (List.class.equals(fieldFromObjectType)) {
                var fieldValueTypeList = (List<Object>) ReflectionUtils.getField(fieldFromObject,
                    objectFromRequestBody);
                addFilePropertiesToList(fileProperties, fieldValueTypeList, fieldFromObject);
                // objects that can contain nested files
              } else if (!fieldFromObjectType.isEnum()) {
                var fieldValue = ReflectionUtils.getField(fieldFromObject, objectFromRequestBody);
                if (Objects.nonNull(fieldValue) && isObjectCanContainFile(fieldValue)) {
                  fillFilePropertiesFromObject(fieldValue, fileProperties);
                }
              }
            }
        );
  }

  private boolean isObjectCanContainFile(Object object) {
    return fileProcessing.getAllowedPackages().stream()
        .anyMatch(packageName -> object.getClass().getName().startsWith(packageName));

  }

  /**
   * Adds a file to the file's property list if the file is non-null.
   *
   * @param fileProperties             list to add files
   * @param fieldFromRequestBodyObject field with type File to be added
   * @param objectFromRequestBody      an object that contains a field with a {@link File} type
   */
  private void addFilePropertyToList(List<FileProperty> fileProperties,
      Field fieldFromRequestBodyObject, Object objectFromRequestBody) {
    var objFieldValue = (File) ReflectionUtils.getField(fieldFromRequestBodyObject,
        objectFromRequestBody);
    if (Objects.nonNull(objFieldValue)) {
      fileProperties.add(new FileProperty(fieldFromRequestBodyObject.getName(), objFieldValue));
    }
  }

  /**
   * Adds a files to the file's property list.
   * <p>
   * Adds a files to the file's property list if objectsFromFieldValue is not empty and contains
   * {@link File} objects. If objectsFromFieldValue is not empty and contains no {@link File}
   * objects, but the object package is on the allowed list, the
   * {@link #fillFilePropertiesFromObject(Object, List)} method is executed for each object.
   *
   * @param fileProperties             list to add files
   * @param objectsFromFieldValue      list of objects from field value
   * @param fieldFromRequestBodyObject field with type list/array
   */
  private void addFilePropertiesToList(List<FileProperty> fileProperties,
      List<Object> objectsFromFieldValue, Field fieldFromRequestBodyObject) {
    var isListNotEmpty =
        Objects.nonNull(objectsFromFieldValue) && !objectsFromFieldValue.isEmpty();
    if (isListNotEmpty) {
      if (isListContainsFiles(objectsFromFieldValue)) {
        objectsFromFieldValue.forEach(object -> {
          var file = (File) object;
          fileProperties.add(new FileProperty(fieldFromRequestBodyObject.getName(), file));
        });
      } else if (isObjectCanContainFile(objectsFromFieldValue.get(0))) {
        objectsFromFieldValue.forEach(
            object -> fillFilePropertiesFromObject(object, fileProperties));
      }
    }
  }

  /**
   * Checks if the input list contains elements of type {@link File}.
   *
   * @param objectsFromFieldValue list of objects to check
   * @return true if the list contains elements of type {@link File}
   */
  private boolean isListContainsFiles(List<Object> objectsFromFieldValue) {
    return File.class.isAssignableFrom(objectsFromFieldValue.get(0).getClass());
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
