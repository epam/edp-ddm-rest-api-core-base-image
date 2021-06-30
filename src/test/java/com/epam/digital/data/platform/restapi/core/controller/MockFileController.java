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

package com.epam.digital.data.platform.restapi.core.controller;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.restapi.core.annotation.HttpRequestContext;
import com.epam.digital.data.platform.restapi.core.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import com.epam.digital.data.platform.restapi.core.service.MockFileService;
import com.epam.digital.data.platform.restapi.core.utils.ResponseResolverUtil;
import java.util.UUID;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/mock-file")
public class MockFileController {

  private MockFileService mockService;

  public MockFileController(MockFileService mockService) {
    this.mockService = mockService;
  }

  @GetMapping
  public ResponseEntity<MockEntityFile> searchEntity(Object searchConditions,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    var response = mockService.search(searchConditions);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @GetMapping("/{id}")
  public ResponseEntity<MockEntityFile> findByIdMockEntity(
      @PathVariable("id") UUID id,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    Request<UUID> request = new Request<>(id, context, securityContext);
    var response = mockService.read(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @PostMapping
  public ResponseEntity<Void> createMockEntity(
      @Valid @RequestBody MockEntityFile mockEntity,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    Request<MockEntityFile> request = new Request<>(mockEntity, context, securityContext);
    var response = mockService.create(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Void> updateMockEntity(
      @PathVariable("id") UUID id,
      @Valid @RequestBody MockEntityFile mockEntity,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    mockEntity.setId(id);
    Request<MockEntityFile> request = new Request<>(mockEntity, context, securityContext);
    var response = mockService.update(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> patchMockEntity(
      @PathVariable("id") UUID id,
      @Valid @RequestBody MockEntityFile mockEntity,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    mockEntity.setId(id);
    Request<MockEntityFile> request = new Request<>(mockEntity, context, securityContext);
    var response = mockService.update(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteByIdMockEntity(
      @PathVariable("id") UUID id,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    MockEntityFile mockEntity = new MockEntityFile();
    mockEntity.setId(id);
    Request<MockEntityFile> request = new Request<>(mockEntity, context, securityContext);
    var response = mockService.delete(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }
}
