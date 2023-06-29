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
import com.epam.digital.data.platform.restapi.core.model.FileProperty;
import com.epam.digital.data.platform.restapi.core.utils.FieldProcessingUtils;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.commons.compress.utils.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;

@RequiredArgsConstructor
@Service
public class FilePropertiesService {

  private final Logger log = LoggerFactory.getLogger(FilePropertiesService.class);

  private final FileProcessing fileProcessing;

  public List<FileProperty> getFileProperties(Object body) {
    var convertedBody = convertToCollection(body);
    log.debug("Request body elements: {}", convertedBody.size());
    List<FileProperty> fileProperties = Lists.newArrayList();
    convertedBody.forEach(object -> fillFilePropertiesFromObject(object, fileProperties));
    log.debug("Total number of files in the request: {}", fileProperties.size());
    return fileProperties;
  }

  public void resetFileProperties(Object body) {
    var convertedBody = convertToCollection(body);
    log.debug("Process-instance-id header is missing, resetting file properties");
    convertedBody.forEach(this::resetFileFields);
    log.debug("File properties set to null");
  }

  @SuppressWarnings("unchecked")
  private Collection<Object> convertToCollection(Object obj) {
    Collection<Object> collection;
    if (Collection.class.isAssignableFrom(obj.getClass())) {
      collection = (Collection<Object>) obj;
    } else {
      collection = singletonList(obj);
    }
    return collection;
  }

  private void fillFilePropertiesFromObject(Object objectFromRequestBody,
      List<FileProperty> fileProperties) {
    Arrays.stream(objectFromRequestBody.getClass().getDeclaredFields())
        .forEach(fieldFromObject -> {
              ReflectionUtils.makeAccessible(fieldFromObject);
              var fieldFromObjectType = fieldFromObject.getType();
              var listFromField = FieldProcessingUtils.convertFieldToListByType(fieldFromObject,
                  objectFromRequestBody, fieldFromObjectType);
              addFilePropertiesToList(listFromField, fieldFromObject, fileProperties);
              if (!fieldFromObjectType.isEnum() && Objects.nonNull(listFromField)
                  && listFromField.isEmpty()) {
                var fieldValue = ReflectionUtils.getField(fieldFromObject,
                    objectFromRequestBody);
                if (Objects.nonNull(fieldValue) && isObjectCanContainFile(fieldValue)) {
                  fillFilePropertiesFromObject(fieldValue, fileProperties);
                }
              }
            }
        );
  }

  private void resetFileFields(Object objectFromResponseBody) {
    Arrays.stream(objectFromResponseBody.getClass().getDeclaredFields())
        .forEach(fieldFromObject -> {
              ReflectionUtils.makeAccessible(fieldFromObject);
              var fieldFromObjectType = fieldFromObject.getType();
              var listFromField = FieldProcessingUtils.convertFieldToListByType(fieldFromObject,
                  objectFromResponseBody, fieldFromObjectType);
              setFileFieldToNull(listFromField, fieldFromObject, objectFromResponseBody);
              if (!fieldFromObjectType.isEnum() && Objects.nonNull(listFromField)
                  && listFromField.isEmpty()) {
                var fieldValue = ReflectionUtils.getField(fieldFromObject,
                    objectFromResponseBody);
                if (Objects.nonNull(fieldValue) && isObjectCanContainFile(fieldValue)) {
                  resetFileFields(fieldValue);
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
   * Sets the file field to null in the given object if the list of objects from the field value is
   * not empty.
   * <p>
   * If the list contains files, the field is set to null directly. If the list contains objects
   * that can potentially contain file fields, the {@link #resetFileFields(Object)} method is
   * executed for each object.
   *
   * @param objectsFromFieldValue  the list of objects obtained from the field value
   * @param fieldFromObject        the field that represents the file object or a list/array
   *                               containing files.
   * @param objectFromResponseBody the object from response body
   */
  private void setFileFieldToNull(List<Object> objectsFromFieldValue, Field fieldFromObject,
      Object objectFromResponseBody) {
    var isListNotEmpty =
        Objects.nonNull(objectsFromFieldValue) && !objectsFromFieldValue.isEmpty();
    if (isListNotEmpty) {
      if (isListContainsFiles(objectsFromFieldValue)) {
        ReflectionUtils.setField(fieldFromObject, objectFromResponseBody, null);
      } else if (isObjectCanContainFile(objectsFromFieldValue.get(0))) {
        objectsFromFieldValue.forEach(this::resetFileFields);
      }
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
   * @param objectsFromFieldValue      list of objects from field value
   * @param fieldFromRequestBodyObject field with type list/array/file
   * @param fileProperties             list to add files
   */
  private void addFilePropertiesToList(List<Object> objectsFromFieldValue,
      Field fieldFromRequestBodyObject, List<FileProperty> fileProperties) {
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
}
