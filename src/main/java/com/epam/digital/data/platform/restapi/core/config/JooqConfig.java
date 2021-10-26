package com.epam.digital.data.platform.restapi.core.config;

import com.epam.digital.data.platform.restapi.core.config.properties.DatabaseProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.jooq.SQLDialect;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
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
  public DefaultConfiguration configuration(DataSourceConnectionProvider connectionProvider) {
    return (DefaultConfiguration) new DefaultConfiguration()
        .derive(connectionProvider)
        .derive(SQLDialect.POSTGRES);
  }
}
