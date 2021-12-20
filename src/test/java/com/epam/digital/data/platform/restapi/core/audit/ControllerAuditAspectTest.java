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

package com.epam.digital.data.platform.restapi.core.audit;

import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.restapi.core.controller.MockController;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.exception.AuditException;
import com.epam.digital.data.platform.restapi.core.service.MockService;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.UUID;

import static com.epam.digital.data.platform.restapi.core.util.ControllerTestUtils.mockResponse;
import static com.epam.digital.data.platform.restapi.core.util.ControllerTestUtils.mockSuccessResponse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@Import(AopAutoConfiguration.class)
@SpringBootTest(classes = {
    MockController.class,
    ControllerAuditAspectTest.MockNonControllerClient.class,
    ControllerAuditAspect.class,
    ApplicationExceptionHandler.class
})
@MockBean(ObjectMapper.class)
@MockBean(TraceProvider.class)
class ControllerAuditAspectTest {

  private static final UUID ENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  @Autowired
  private MockController controller;
  @Autowired
  private ApplicationExceptionHandler applicationExceptionHandler;
  @Autowired
  private MockNonControllerClient nonControllerClient;

  @MockBean
  private MockService mockService;
  @MockBean
  private RestAuditEventsFacade restAuditEventsFacade;

  @Mock
  private RequestContext mockRequestContext;
  @Mock
  private SecurityContext mockSecurityContext;

  @BeforeEach
  void beforeEach() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  void expectAuditAspectBeforeAndAfterGetMethodWhenNoException() {
    when(mockService.read(any())).thenReturn(mockSuccessResponse());

    controller.findByIdMockEntity(ENTITY_ID, mockRequestContext, mockSecurityContext);

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnGetMethod() {
    when(mockService.read(any())).thenThrow(new RuntimeException());

    assertThrows(
        RuntimeException.class,
        () -> controller.findByIdMockEntity(ENTITY_ID, mockRequestContext, mockSecurityContext));

    verify(restAuditEventsFacade)
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectExceptionWhenControllerHasUnsupportedMappingAnnotation() {
    assertThrows(
        AuditException.class,
        () -> controller.findById(ENTITY_ID, mockRequestContext, mockSecurityContext));
  }

  @Test
  void expectExceptionWhenControllerHasMoreThenOneMappingAnnotation() {
    assertThrows(
        AuditException.class,
        () -> controller.patchPutMockEntity(
            ENTITY_ID, mockPayload(), mockRequestContext, mockSecurityContext));
  }

  @Test
  void expectAuditAspectBeforeAndAfterPostMethodWhenNoException() {
    when(mockService.create(any())).thenReturn(mockSuccessResponse());

    controller.createMockEntity(mockPayload(), mockRequestContext, mockSecurityContext);

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnPostMethod() {
    when(mockService.create(any())).thenThrow(new RuntimeException());
    MockEntity mockEntity = mockPayload();

    assertThrows(
        RuntimeException.class,
        () -> controller.createMockEntity(mockEntity, mockRequestContext, mockSecurityContext));

    verify(restAuditEventsFacade)
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectBeforeAndAfterPutMethodWhenNoException() {
    when(mockService.update(any())).thenReturn(mockSuccessResponse());

    controller.updateMockEntity(ENTITY_ID, mockPayload(), mockRequestContext, mockSecurityContext);

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnPutMethod() {
    when(mockService.update(any())).thenReturn(mockResponse(Status.PROCEDURE_ERROR));
    MockEntity mockEntity = mockPayload();

    assertThrows(
        RuntimeException.class,
        () ->
            controller.updateMockEntity(
                ENTITY_ID,
                mockEntity,
                mockRequestContext,
                mockSecurityContext));

    verify(restAuditEventsFacade)
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectBeforeAndAfterPatchMethodWhenNoException() {
    when(mockService.update(any())).thenReturn(mockSuccessResponse());

    controller.patchMockEntity(ENTITY_ID, mockPayload(), mockRequestContext, mockSecurityContext);

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnPatchMethod() {
    when(mockService.update(any())).thenReturn(mockResponse(Status.PROCEDURE_ERROR));
    MockEntity mockEntity = mockPayload();

    assertThrows(
        RuntimeException.class,
        () ->
            controller.patchMockEntity(
                ENTITY_ID,
                mockEntity,
                mockRequestContext,
                mockSecurityContext));

    verify(restAuditEventsFacade)
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectBeforeAndAfterDeleteMethodWhenNoException() {
    when(mockService.delete(any())).thenReturn(mockSuccessResponse());

    controller.deleteByIdMockEntity(ENTITY_ID, mockRequestContext, mockSecurityContext);

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnDeleteMethod() {
    when(mockService.delete(any())).thenThrow(new RuntimeException());

    assertThrows(
        RuntimeException.class,
        () -> controller.deleteByIdMockEntity(ENTITY_ID, mockRequestContext, mockSecurityContext));

    verify(restAuditEventsFacade)
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectBeforeAndAfterSearchMethodWhenNoException() {
    when(mockService.search(any())).thenReturn(mockSuccessResponse());

    controller.searchEntity(new MockEntity(), mockRequestContext, mockSecurityContext);

    verify(restAuditEventsFacade, times(2))
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectOnlyBeforeWhenExceptionOnSearchMethod() {
    when(mockService.search(any())).thenThrow(new RuntimeException());

    assertThrows(
        RuntimeException.class,
        () -> controller.findByIdMockEntity(ENTITY_ID, mockRequestContext, mockSecurityContext));

    verify(restAuditEventsFacade)
        .sendRestAudit(any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  void expectAuditAspectNonCalledIfNonRestControllerCall() {
    nonControllerClient.postNonController();

    verifyNoInteractions(restAuditEventsFacade);
  }

  @Test
  void expectAuditAspectBeforeGetAndAfterExceptionHandler(){
    applicationExceptionHandler.handleException(new RuntimeException());

    verify(restAuditEventsFacade).sendExceptionAudit(any());
  }

  private MockEntity mockPayload() {
    MockEntity stub = new MockEntity();
    stub.setConsentId(ENTITY_ID);
    stub.setPersonFullName("some name");
    return stub;
  }

  @TestComponent
  public static class MockNonControllerClient {

    @PostMapping
    public MockEntity postNonController() {
      return new MockEntity();
    }
  }
}
