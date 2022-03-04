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

import com.epam.digital.data.platform.starter.database.config.JooqConfig;
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
