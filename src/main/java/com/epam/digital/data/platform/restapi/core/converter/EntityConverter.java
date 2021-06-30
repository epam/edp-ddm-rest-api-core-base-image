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

package com.epam.digital.data.platform.restapi.core.converter;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class EntityConverter<T> {

  private static final String CURR_USER = "curr_user";
  private static final String SOURCE_SYSTEM = "source_system";
  private static final String SOURCE_APPLICATION = "source_application";
  private static final String SOURCE_PROCESS = "source_process";
  private static final String PROCESS_DEFINITION_ID = "source_process_definition_id";
  private static final String PROCESS_INSTANCE_ID = "source_process_instance_id";
  private static final String BUSINESS_ACTIVITY = "business_activity";
  private static final String ACTIVITY_INSTANCE_ID = "source_activity_instance_id";

  private static final String DIGITAL_SIGNATURE = "digital_sign";
  private static final String DIGITAL_SIGNATURE_DERIVED = "digital_sign_derived";

  private static final String DIGITAL_SIGNATURE_CHECKSUM = "ddm_digital_sign_checksum";
  private static final String DIGITAL_SIGNATURE_DERIVED_CHECKSUM = "ddm_digital_sign_derived_checksum";

  private final ObjectMapper objectMapper;

  public EntityConverter(@Qualifier("jooqMapper") ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  public String getUuidOfEntity(T entity, String column) {
    Map<String, Object> entityMap = entityToMap(entity);
    return entityMap.get(column).toString();
  }

  public Map<String, Object> entityToMap(T entity) {
    TypeReference<Map<String, Object>> reference = new TypeReference<>() {};
    Map<String, Object> entityMap = objectMapper.convertValue(entity, reference);
    entityMap.putAll(listsToStrings(entityMap));
    entityMap.putAll(mapsToStrings(entityMap));
    return entityMap;
  }

  public Map<String, String> buildSysValues(String userId, Request<T> input) {
    RequestContext context = input.getRequestContext();
    SecurityContext securityContext = input.getSecurityContext();

    Map<String, String> values = new HashMap<>();
    values.put(CURR_USER, userId);
    values.put(SOURCE_SYSTEM, context.getSystem());
    values.put(SOURCE_APPLICATION, context.getApplication());

    if (context.getBusinessProcess() != null) {
      values.put(SOURCE_PROCESS, context.getBusinessProcess());
    }
    if (context.getBusinessActivity() != null) {
      values.put(BUSINESS_ACTIVITY, context.getBusinessActivity());
    }
    if (context.getBusinessProcessDefinitionId() != null) {
      values.put(PROCESS_DEFINITION_ID, context.getBusinessProcessDefinitionId());
    }
    if (context.getBusinessProcessInstanceId() != null) {
      values.put(PROCESS_INSTANCE_ID, context.getBusinessProcessInstanceId());
    }
    if (context.getBusinessActivityInstanceId() != null) {
      values.put(ACTIVITY_INSTANCE_ID, context.getBusinessActivityInstanceId());
    }
    if (securityContext.getDigitalSignature() != null) {
      values.put(DIGITAL_SIGNATURE, securityContext.getDigitalSignature());
    }
    if (securityContext.getDigitalSignatureDerived() != null) {
      values.put(DIGITAL_SIGNATURE_DERIVED, securityContext.getDigitalSignatureDerived());
    }
    if (securityContext.getDigitalSignatureChecksum() != null) {
      values.put(DIGITAL_SIGNATURE_CHECKSUM, securityContext.getDigitalSignatureChecksum());
    }
    if (securityContext.getDigitalSignatureDerivedChecksum() != null) {
      values.put(DIGITAL_SIGNATURE_DERIVED_CHECKSUM, securityContext.getDigitalSignatureDerivedChecksum());
    }

    return values;
  }

  private Map<String, Object> listsToStrings(Map<String, Object> entityMap) {
    return entityMap.entrySet().stream()
        .filter(entry -> entry.getValue() instanceof List)
        .map(entry -> {
          var list = (List<?>) entry.getValue();
          if(!list.isEmpty() && list.get(0) instanceof LinkedHashMap) {
            var listOfStrings = ((List<LinkedHashMap>)entry.getValue()).stream()
                .map(this::toCompatibleStringWithInnerMap)
                .collect(Collectors.toList());
            entry.setValue(listOfStrings);
          }
          return Map.entry(entry.getKey(), toCompatibleString((Collection<?>) entry.getValue()));
        })
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private Map<String, Object> mapsToStrings(Map<String, Object> entityMap) {
    return entityMap.entrySet().stream()
        .filter(entry -> entry.getValue() instanceof LinkedHashMap)
        .map(
            entry ->
                Map.entry(
                    entry.getKey(), toCompatibleString((Map<?, ?>) entry.getValue())))
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private String toCompatibleString(Map<?, ?> map) {
    return "("
        + map.values().stream()
            .map(Objects::toString)
            .collect(Collectors.joining(","))
        + ")";
  }

  private String toCompatibleStringWithInnerMap(Map<?, ?> map) {
    return "\"(" + map.values().stream().map(Object::toString).collect(Collectors.joining(",")) + ")\"";
  }

  private String toCompatibleString(Collection<?> list) {
    return "{" + StringUtils.collectionToCommaDelimitedString(list) + "}";
  }
}
