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

package com.epam.digital.data.platform.restapi.core.advice;

import static com.epam.digital.data.platform.restapi.core.utils.Header.X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID;
import static java.util.Collections.singletonList;

import com.epam.digital.data.platform.restapi.core.exception.FileNotExistsException;
import com.epam.digital.data.platform.restapi.core.exception.MandatoryHeaderMissingException;
import com.epam.digital.data.platform.restapi.core.service.FileService;
import java.util.ArrayList;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class FileResponseBodyAdvice implements ResponseBodyAdvice {

  private final FileService fileService;

  public FileResponseBodyAdvice(
      FileService fileService) {
    this.fileService = fileService;
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

    var notFound = new ArrayList<String>();

    fileService.getFileProperties(body).forEach(
        f -> {
          var success = fileService.retrieve(getInstanceId(request), f.getValue());
          if (!success) {
            notFound.add(f.getName());
          }
        }
    );

    if (!notFound.isEmpty()) {
      throw new FileNotExistsException("Files not found in ceph bucket", notFound);
    }

    return body;
  }

  private String getInstanceId(ServerHttpRequest request) {
    var vals = request.getHeaders().get(X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName());
    if (vals == null || vals.size() != 1) {
      // should never happen
      throw new MandatoryHeaderMissingException(
          singletonList(X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName()));
    }
    return vals.get(0);
  }
}
