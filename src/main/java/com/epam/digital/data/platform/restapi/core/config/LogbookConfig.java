package com.epam.digital.data.platform.restapi.core.config;

import com.epam.digital.data.platform.restapi.core.filter.FiltersOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zalando.logbook.Logbook;
import org.zalando.logbook.autoconfigure.LogbookProperties;
import org.zalando.logbook.servlet.LogbookFilter;

import javax.servlet.DispatcherType;

@Configuration
public class LogbookConfig {

  private final LogbookProperties logbookProperties;

  public LogbookConfig(LogbookProperties logbookProperties) {
    this.logbookProperties = logbookProperties;
  }

  @Bean
  @ConditionalOnProperty(
      name = {"logbook.filter.enabled"},
      havingValue = "true",
      matchIfMissing = true)
  public FilterRegistrationBean<LogbookFilter> logbookFilter(Logbook logbook) {
    var filter =
        new LogbookFilter(logbook)
            .withFormRequestMode(logbookProperties.getFilter().getFormRequestMode());
    var registration = new FilterRegistrationBean<>(filter);
    registration.setName("logbookFilter");
    registration.setDispatcherTypes(
            DispatcherType.REQUEST, DispatcherType.ASYNC);
    registration.setOrder(FiltersOrder.LOGBOOK_FILTER);
    return registration;
  }
}