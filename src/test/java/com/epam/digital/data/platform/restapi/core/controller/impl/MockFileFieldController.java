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

package com.epam.digital.data.platform.restapi.core.controller.impl;

import com.epam.digital.data.platform.model.core.file.FileResponseDto;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.annotation.HttpRequestContext;
import com.epam.digital.data.platform.restapi.core.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.restapi.core.audit.AuditableController;
import com.epam.digital.data.platform.restapi.core.model.FileRequestDto;
import com.epam.digital.data.platform.restapi.core.utils.ResponseResolverUtil;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.mockito.Mockito.mock;

@RestController
@RequestMapping("/files/mock-file")
public class MockFileFieldController {

  private final MockReadFileService mockService;

  public MockFileFieldController(MockReadFileService mockService) {
    this.mockService = mockService;
  }

  @AuditableController
  @GetMapping(value = "/{id}/scan-copy/{fileId}", produces = { MediaType.APPLICATION_JSON_VALUE })
  public ResponseEntity<FileResponseDto> findFileDto(
          @PathVariable("id") java.util.UUID id,
          @PathVariable("fileId") String fileId,
          @HttpRequestContext RequestContext context,
          @HttpSecurityContext SecurityContext securityContext) {
    var fileRequestDto = new FileRequestDto<>(id, fileId);
    var request = new Request<>(fileRequestDto, context, securityContext);
    var response = mockService.requestDto(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @TestComponent
  public static class MockReadFileService {

    public Response<FileResponseDto> requestDto(Request<FileRequestDto<UUID>> input) {
      return mock(Response.class);
    }
  }
}
