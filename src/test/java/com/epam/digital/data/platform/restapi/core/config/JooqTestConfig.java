package com.epam.digital.data.platform.restapi.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.tools.jdbc.MockConnection;
import org.jooq.tools.jdbc.MockDataProvider;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JooqTestConfig {

  @Bean
  public DSLContext context() {
    MockDataProvider provider = new TestDataProvider();
    MockConnection connection = new MockConnection(provider);
    return DSL.using(connection, SQLDialect.POSTGRES);
  }

  @Bean
  public DataSource dataSource() {
    return Mockito.mock(DataSource.class);
  }

  @Bean
  public ObjectMapper jooqMapper() {
    return new JooqConfig().jooqMapper();
  }
}
