package com.epam.digital.data.platform.restapi.core.config;

import static org.mockito.Mockito.mock;

import com.epam.digital.data.platform.restapi.core.service.FileService;
import com.epam.digital.data.platform.restapi.core.service.RestAuditEventsFacade;
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
  public FileService fileService() {
    return mock(FileService.class);
  }
}
