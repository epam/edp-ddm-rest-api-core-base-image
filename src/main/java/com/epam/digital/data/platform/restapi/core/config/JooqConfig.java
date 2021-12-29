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

import com.epam.digital.data.platform.restapi.core.config.properties.DatabaseProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import javax.sql.DataSource;
import org.jooq.Converter;
import org.jooq.ConverterProvider;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.DefaultConverterProvider;
import org.jooq.impl.EnumConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

@Configuration
@EnableConfigurationProperties(DatabaseProperties.class)
public class JooqConfig {

  @Bean
  public DataSource datasource(DatabaseProperties databaseProperties) {
    HikariConfig configuration = new HikariConfig();
    configuration.setJdbcUrl(databaseProperties.getUrl());
    configuration.setUsername(databaseProperties.getUsername());
    configuration.setPassword(databaseProperties.getPassword());
    configuration.setConnectionTimeout(databaseProperties.getConnectionTimeout());
    return new HikariDataSource(configuration);
  }

  @Bean
  public DataSourceConnectionProvider connectionProvider(DataSource dataSource) {
    return new DataSourceConnectionProvider
        (new TransactionAwareDataSourceProxy(dataSource));
  }

  @Bean("jooqMapper")
  public ObjectMapper jooqMapper() {
    final ObjectMapper mapper = new ObjectMapper();
    mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    return mapper;
  }

  @Bean
  public DefaultConfiguration configuration(DataSourceConnectionProvider connectionProvider,
      ConverterProvider converterProvider) {
    return (DefaultConfiguration) new DefaultConfiguration()
        .derive(connectionProvider)
        .derive(SQLDialect.POSTGRES)
        .set(converterProvider);
  }

  @Bean
  public ConverterProvider converterProvider() {
    return new ConverterProvider() {
      final ConverterProvider defaultConverterProvider = new DefaultConverterProvider();

      @Override
      public <T, U> Converter<T, U> provide(Class<T> tType, Class<U> uType) {
        if (uType == LocalDate.class) {
          return (Converter<T, U>) Converter.ofNullable(Date.class, LocalDate.class,
              Date::toLocalDate, Date::valueOf);
        } else if (uType == LocalTime.class) {
          return (Converter<T, U>) Converter.ofNullable(Time.class, LocalTime.class,
              Time::toLocalTime, Time::valueOf);
        } else if (uType == LocalDateTime.class) {
          return (Converter<T, U>) Converter.ofNullable(Timestamp.class, LocalDateTime.class,
              Timestamp::toLocalDateTime, Timestamp::valueOf);
        } else if (Enum.class.isAssignableFrom(uType)) {
          return new EnumConverter(tType, uType);
        } else {
          return defaultConverterProvider.provide(tType, uType);
        }
      }
    };
  }
}
