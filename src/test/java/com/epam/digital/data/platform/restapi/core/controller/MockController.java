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
import com.epam.digital.data.platform.restapi.core.audit.AuditableController;
import com.epam.digital.data.platform.restapi.core.annotation.HttpRequestContext;
import com.epam.digital.data.platform.restapi.core.annotation.HttpSecurityContext;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityCreateList;
import com.epam.digital.data.platform.restapi.core.service.MockService;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.utils.ResponseResolverUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.UUID;

@RestController
@RequestMapping("/mock")
public class MockController {

  private final MockService mockService;

  public MockController(MockService mockService) {
    this.mockService = mockService;
  }

  @AuditableController
  @GetMapping
  public ResponseEntity<MockEntity> searchEntity(Object searchConditions,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    var response = mockService.search(searchConditions);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @AuditableController
  @GetMapping("/{id}")
  public ResponseEntity<MockEntity> findByIdMockEntity(
      @PathVariable("id") UUID id,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    Request<UUID> request = new Request<>(id, context, securityContext);
    var response = mockService.read(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @AuditableController
  @PostMapping
  public ResponseEntity<Void> createMockEntity(
      @Valid @RequestBody MockEntity mockEntity,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    Request<MockEntity> request = new Request<>(mockEntity, context, securityContext);
    var response = mockService.create(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @AuditableController
  @PutMapping("/{id}")
  public ResponseEntity<Void> updateMockEntity(
      @PathVariable("id") UUID id,
      @Valid @RequestBody MockEntity mockEntity,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    mockEntity.setConsentId(id);
    Request<MockEntity> request = new Request<>(mockEntity, context, securityContext);
    var response = mockService.update(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @AuditableController
  @PatchMapping("/{id}")
  public ResponseEntity<Void> patchMockEntity(
      @PathVariable("id") UUID id,
      @Valid @RequestBody MockEntity mockEntity,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    mockEntity.setConsentId(id);
    Request<MockEntity> request = new Request<>(mockEntity, context, securityContext);
    var response = mockService.update(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @AuditableController
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteByIdMockEntity(
      @PathVariable("id") UUID id,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    MockEntity mockEntity = new MockEntity();
    mockEntity.setConsentId(id);
    Request<MockEntity> request = new Request<>(mockEntity, context, securityContext);
    var response = mockService.delete(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @AuditableController
  @RequestMapping(value = "/test/{id}", method = RequestMethod.GET)
  public ResponseEntity<MockEntity> findById(
      @PathVariable("id") UUID id,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    Request<UUID> request = new Request<>(id, context, securityContext);
    var response = mockService.read(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @AuditableController
  @PatchMapping("/test/{id}")
  @PutMapping("/test/{id}")
  public ResponseEntity<Void> patchPutMockEntity(
      @PathVariable("id") UUID id,
      @Valid @RequestBody MockEntity mockEntity,
      @HttpRequestContext RequestContext context,
      @HttpSecurityContext SecurityContext securityContext) {
    mockEntity.setConsentId(id);
    Request<MockEntity> request = new Request<>(mockEntity, context, securityContext);
    var response = mockService.update(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }

  @AuditableController
  @PostMapping("/list")
  public ResponseEntity<Void> createListMockEntity(
          @Valid @RequestBody MockEntityCreateList mockEntityCreateList,
          @HttpRequestContext RequestContext context,
          @HttpSecurityContext SecurityContext securityContext) {
    Request<MockEntityCreateList> request = new Request<>(mockEntityCreateList, context, securityContext);
    var response = mockService.createList(request);
    return ResponseResolverUtil.getHttpResponseFromKafka(response);
  }
}
