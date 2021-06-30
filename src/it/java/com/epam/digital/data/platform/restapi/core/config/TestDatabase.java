package com.epam.digital.data.platform.restapi.core.config;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import java.sql.Connection;
import java.sql.SQLException;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@AutoConfigureEmbeddedDatabase(provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY)
@Configuration
public class TestDatabase {

  @Autowired
  private DataSource dataSource;

  @PostConstruct
  public void init() throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(connection, new ClassPathResource("init_db.sql"));
    }
  }
}
