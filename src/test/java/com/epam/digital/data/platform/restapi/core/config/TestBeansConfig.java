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

package com.epam.digital.data.platform.restapi.core.config;

import static org.mockito.Mockito.mock;

import com.epam.digital.data.platform.restapi.core.service.FileService;
import com.epam.digital.data.platform.restapi.core.audit.RestAuditEventsFacade;
import com.epam.digital.data.platform.restapi.core.service.MockFileService;
import com.epam.digital.data.platform.restapi.core.service.MockService;
import com.epam.digital.data.platform.restapi.core.service.TraceProvider;
import com.epam.digital.data.platform.starter.actuator.livenessprobe.LivenessStateHandler;
import com.epam.digital.data.platform.starter.audit.service.AuditService;
import com.epam.digital.data.platform.starter.security.config.SecurityProperties;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@ComponentScan(value = "com.epam.digital.data.platform.restapi.core",
        excludeFilters = {@ComponentScan.Filter(
                type = FilterType.CUSTOM,
                classes = {TypeExcludeFilter.class}
        )}
)
@TestConfiguration
public class TestBeansConfig {

  @Bean
  public LivenessStateHandler livenessStateHandler() {
    return mock(LivenessStateHandler.class);
  }

  @Bean
  public AuditService auditService() {
    return mock(AuditService.class);
  }

  @Bean
  public RestAuditEventsFacade auditServiceFacade() {
    return mock(RestAuditEventsFacade.class);
  }

  @Bean
  public UnauthorizedRequestHandler unauthorizedRequestHandler() {
    return mock(UnauthorizedRequestHandler.class);
  }

  @Bean
  public SecurityProperties securityProperties() {
    return mock(SecurityProperties.class);
  }

  @Bean
  public WebConfigProperties webConfigProperties() {
    return mock(WebConfigProperties.class);
  }

  @Bean
  public FileService fileService() {
    return mock(FileService.class);
  }

  @Bean
  public MockService mockService() {
    return mock(MockService.class);
  }

  @Bean
  public MockFileService mockFileService() {
    return mock(MockFileService.class);
  }

  @Bean
  public TraceProvider traceProvider() {
    return mock(TraceProvider.class);
  }
}
