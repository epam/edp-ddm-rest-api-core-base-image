package com.epam.digital.data.platform.restapi.core.aspect;

import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.exception.ApplicationExceptionHandler;
import com.epam.digital.data.platform.restapi.core.service.RestAuditEventsFacade;
import com.epam.digital.data.platform.restapi.core.controller.MockController;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.service.MockService;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.starter.security.exception.JwtParsingException;
import com.epam.digital.data.platform.starter.security.jwt.TokenParser;
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
    ApplicationExceptionHandler.class,
    TokenParser.class
})
class ControllerAuditAspectTest {

  private static final UUID ENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");

  @Autowired
  private MockController controller;
  @Autowired
  private ApplicationExceptionHandler applicationExceptionHandler;
  @Autowired
  private MockNonControllerClient nonControllerClient;
  @Autowired
  private TokenParser tokenParser;

  @MockBean
  private ObjectMapper objectMapper;
  @MockBean
  private MockService mockService;
  @MockBean
  private RestAuditEventsFacade restAuditEventsFacade;
  @MockBean
  private TraceProvider traceProvider;

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

    verify(restAuditEventsFacade).sendExceptionAudit(any(), any());
  }

  @Test
  void expectAuditAspectWhenExceptionWhileTokenParsing() {
    assertThrows(
        JwtParsingException.class,
        () -> tokenParser.parseClaims("incorrectToken"));

    verify(restAuditEventsFacade).auditInvalidAccessToken();
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
