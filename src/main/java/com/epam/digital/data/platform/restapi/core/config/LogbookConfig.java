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