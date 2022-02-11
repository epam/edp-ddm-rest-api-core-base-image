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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.epam.digital.data.platform.dso.api.dto.SignRequestDto;
import com.epam.digital.data.platform.dso.api.dto.SignResponseDto;
import com.epam.digital.data.platform.dso.api.dto.VerificationRequestDto;
import com.epam.digital.data.platform.dso.api.dto.VerificationResponseDto;
import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import com.epam.digital.data.platform.integration.ceph.model.CephObject;
import com.epam.digital.data.platform.integration.ceph.model.CephObjectMetadata;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.restapi.core.audit.AuditableDatabaseOperation;
import com.epam.digital.data.platform.restapi.core.audit.AuditableDatabaseOperation.Operation;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;

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
    mockCephService.exist("", "");
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
    public Optional<CephObject> get(String s, String s1) {
      return Optional.empty();
    }

    @Override
    public Optional<String> getAsString(String s, String s1) {
      return Optional.empty();
    }

    @Override
    public void put(String s, String s1, String s2) {}

    @Override
    public CephObjectMetadata put(
        String s, String s1, String s2, Map<String, String> map, InputStream inputStream) {
      return null;
    }

    @Override
    public void delete(String s, Set<String> set) {}

    @Override
    public Boolean exist(String s, Set<String> set) {
      return false;
    }

    @Override
    public Boolean exist(String s, String s1) {
      return false;
    }

    @Override
    public Set<String> getKeys(String s, String s1) {
      return null;
    }

    @Override
    public List<CephObjectMetadata> getMetadata(String s, Set<String> set) {
      return null;
    }
  }

  @TestComponent
  static class MockDigitalSealRestClient implements DigitalSealRestClient {

    @Override
    public VerificationResponseDto verify(VerificationRequestDto VerificationRequestDto) {
      return null;
    }

    @Override
    public SignResponseDto sign(SignRequestDto signRequest) {
      return null;
    }

    @Override
    public SignResponseDto sign(SignRequestDto signRequestDto, HttpHeaders httpHeaders) {
      return null;
    }
  }

  @TestComponent
  static class MockDbClient {

    @AuditableDatabaseOperation(Operation.READ)
    public void read() {}
  }
}
