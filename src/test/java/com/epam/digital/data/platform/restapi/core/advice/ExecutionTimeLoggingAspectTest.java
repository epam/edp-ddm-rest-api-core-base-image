package com.epam.digital.data.platform.restapi.core.advice;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.epam.digital.data.platform.dso.api.dto.SignRequestDto;
import com.epam.digital.data.platform.dso.api.dto.SignResponseDto;
import com.epam.digital.data.platform.dso.api.dto.VerifyRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerifyResponseDto;
import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import com.epam.digital.data.platform.integration.ceph.dto.CephObject;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.restapi.core.annotation.DatabaseOperation;
import com.epam.digital.data.platform.restapi.core.annotation.DatabaseOperation.Operation;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;

@Import({AopAutoConfiguration.class})
@SpringBootTest(
    classes = {
      ExecutionTimeLoggingAspectTest.MockCephService.class,
      ExecutionTimeLoggingAspectTest.MockDigitalSealRestClient.class,
      ExecutionTimeLoggingAspect.class,
      ExecutionTimeLoggingAspectTest.MockDbClient.class,
    })
class ExecutionTimeLoggingAspectTest {

  @Autowired
  private MockCephService mockCephService;
  @Autowired
  private MockDigitalSealRestClient mockDigitalSealRestClient;
  @Autowired
  private MockDbClient mockDbClient;

  @SpyBean
  private ExecutionTimeLoggingAspect executionTimeLoggingAspect;

  @Test
  void expectCephServiceAspectCalled() throws Throwable {
    mockCephService.deleteObject("", "");
    verify(executionTimeLoggingAspect).logCephCommunicationTime(any());
  }

  @Test
  void expectDsoServiceAspectCalled() throws Throwable {
    mockDigitalSealRestClient.sign(null);

    verify(executionTimeLoggingAspect).logDsoCommunicationTime(any());
  }

  @Test
  void expectDbAspectCalled() throws Throwable {
    mockDbClient.read();

    verify(executionTimeLoggingAspect).logDbCommunicationTime(any());
  }

  @TestComponent
  static class MockCephService implements CephService {

    @Override
    public Optional<String> getContent(String s, String s1) {
      return Optional.empty();
    }

    @Override
    public Optional<CephObject> getObject(String s, String s1) {
      return Optional.empty();
    }

    @Override
    public void putContent(String s, String s1, String s2) {}

    @Override
    public void putObject(String s, String s1, CephObject cephObject) {}

    @Override
    public void deleteObject(String s, String s1) {}

    @Override
    public boolean doesObjectExist(String s, String s1) {
      return false;
    }
  }

  @TestComponent
  static class MockDigitalSealRestClient implements DigitalSealRestClient {

    @Override
    public VerifyResponseDto verify(VerifyRequestDto verifyRequestDto) {
      return null;
    }

    @Override
    public SignResponseDto sign(SignRequestDto signRequest) {
      return null;
    }
  }

  @TestComponent
  static class MockDbClient {

    @DatabaseOperation(Operation.READ)
    public void read() {}
  }
}
