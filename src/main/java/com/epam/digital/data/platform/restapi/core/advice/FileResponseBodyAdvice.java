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

package com.epam.digital.data.platform.restapi.core.advice;

import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_ROOT_BUSINESS_PROCESS_INSTANCE_ID;

import com.epam.digital.data.platform.restapi.core.exception.FileNotExistsException;
import com.epam.digital.data.platform.restapi.core.model.FileProperty;
import com.epam.digital.data.platform.restapi.core.service.FilePropertiesService;
import com.epam.digital.data.platform.restapi.core.service.FileService;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class FileResponseBodyAdvice implements ResponseBodyAdvice {

  private final FileService fileService;
  private final FilePropertiesService filePropertiesService;

  public FileResponseBodyAdvice(FileService fileService,
      FilePropertiesService filePropertiesService) {
    this.fileService = fileService;
    this.filePropertiesService = filePropertiesService;
  }

  @Override
  public boolean supports(MethodParameter returnType, Class converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType,
      MediaType selectedContentType, Class selectedConverterType, ServerHttpRequest request,
      ServerHttpResponse response) {
    if (body == null) {
      return null;
    }

    var rootProcessInstanceId = getRootProcessInstanceId(request);
    if (Objects.isNull(rootProcessInstanceId)) {
      filePropertiesService.resetFileProperties(body);
    } else {
      var fileProperties = filePropertiesService.getFileProperties(body);
      storeFilesToLowcodeCephBucket(fileProperties, rootProcessInstanceId);
    }
    return body;
  }

  private String getRootProcessInstanceId(ServerHttpRequest request) {
    return Optional.ofNullable(
            request.getHeaders().get(X_SOURCE_ROOT_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName()))
        .map(val -> val.get(0))
        .orElse(null);
  }

  private void storeFilesToLowcodeCephBucket(List<FileProperty> fileProperties, String rootProcessInstanceId) {
    var notFoundFileNames = new ArrayList<String>();
    fileProperties.forEach(fileProperty -> {
      var isFileFound = fileService.retrieve(rootProcessInstanceId, fileProperty.getValue());
      if (!isFileFound) {
        notFoundFileNames.add(fileProperty.getName());
      }
    });
    if (!notFoundFileNames.isEmpty()) {
      throw new FileNotExistsException("Files not found in ceph bucket", notFoundFileNames);
    }
  }
}
