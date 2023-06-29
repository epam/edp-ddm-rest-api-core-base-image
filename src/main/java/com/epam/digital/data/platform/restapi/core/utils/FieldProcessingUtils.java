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

package com.epam.digital.data.platform.restapi.core.utils;

import com.epam.digital.data.platform.model.core.kafka.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import org.apache.commons.compress.utils.Lists;
import org.springframework.util.ReflectionUtils;

/**
 * Utility class for field processing operations.
 */
public class FieldProcessingUtils {

  private static final String FIELD_TYPE_FILE = "file";
  private static final String FIELD_TYPE_ARRAY = "array";
  private static final String FIELD_TYPE_LIST = "list";
  private static final String FIELD_TYPE_OTHER = "other";

  /**
   * Mapping of field types to corresponding processing functions.
   */
  @SuppressWarnings("unchecked")
  private static final Map<String, BiFunction<Field, Object, List<Object>>> FUNCTION_BY_FIELD_TYPE =
      Map.of(
          FIELD_TYPE_FILE, (field, objectFromBody) -> {
            var objFieldValue = ReflectionUtils.getField(field, objectFromBody);
            return Objects.nonNull(objFieldValue) ? List.of(objFieldValue) : Lists.newArrayList();
          },
          FIELD_TYPE_ARRAY, (field, objectFromBody) -> {
            var arrayField = (Object[]) ReflectionUtils.getField(field, objectFromBody);
            return Objects.nonNull(arrayField) ? List.of(arrayField) : Lists.newArrayList();
          },
          FIELD_TYPE_LIST,
          (field, objectFromBody) -> (List<Object>) ReflectionUtils.getField(field, objectFromBody)
      );

  private FieldProcessingUtils() {
  }

  /**
   * Retrieves the list of objects from the specified field of the given object, based on the field
   * type.
   * <p>
   * The field type can be one of the following:
   * <ul>
   * <li>"file": The field represents a file object. The value of the field will be wrapped in a
   * list.</li>
   * <li>"array": The field represents an array. The array elements will be converted to a
   * list.</li>
   * <li>"list": The field represents a list. The value of the field will be returned as is.</li>
   * <li>"other": The field represents a type other than file, array, or list. An empty list will be
   * returned.</li>
   * </ul>
   *
   * @param field          the field to retrieve the objects from
   * @param objectFromBody the object containing the field
   * @param fieldType      the class representing the field type
   * @return the list of objects obtained from the field
   */
  public static List<Object> convertFieldToListByType(Field field, Object objectFromBody,
      Class<?> fieldType) {
    var definedFieldType = defineFieldType(fieldType);
    return FUNCTION_BY_FIELD_TYPE.getOrDefault(definedFieldType, (f, o) -> Collections.emptyList())
        .apply(field, objectFromBody);
  }

  /**
   * Defines the field type based on the specified class.
   *
   * @param fieldType the class representing the field type
   * @return the defined field type as a string
   */
  private static String defineFieldType(Class<?> fieldType) {
    // check if the current field is of type com.epam.digital.data.platform.model.core.kafka.File
    if (File.class.equals(fieldType)) {
      return FIELD_TYPE_FILE;
      // check if the current field represents an array class and does not consist of primitive elements
    } else if (fieldType.isArray() && !fieldType.getComponentType().isPrimitive()) {
      return FIELD_TYPE_ARRAY;
      // check if the current field is of type java.util.List
    } else if (List.class.equals(fieldType)) {
      return FIELD_TYPE_LIST;
    } else {
      return FIELD_TYPE_OTHER;
    }
  }
}
