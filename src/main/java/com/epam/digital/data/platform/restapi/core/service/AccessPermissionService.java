package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.restapi.core.utils.JwtClaimsUtils;
import com.epam.digital.data.platform.restapi.core.utils.SQLExceptionResolverUtil;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class AccessPermissionService<O> {

  private static final String PERMISSION_CHECK_SQL_STRING =
      "select f_check_permissions(?, ?, ?::type_operation, ?);";
  private static final String SEARCH_TYPE_OPERATION = "S";

  private final DataSource dataSource;
  private final ObjectMapper objectMapper;

  public AccessPermissionService(
      DataSource dataSource, @Qualifier("jooqMapper") ObjectMapper objectMapper) {
    this.dataSource = dataSource;
    this.objectMapper = objectMapper;
  }

  public boolean hasReadAccess(String tableName, JwtClaimsDto userClaims, Class<O> entityType) {
    List<String> fields = getRequestedFields(entityType);
    List<String> userRoles = JwtClaimsUtils.getRoles(userClaims);
    try (Connection connection = dataSource.getConnection();
        CallableStatement statement = connection.prepareCall(PERMISSION_CHECK_SQL_STRING)) {
      Array userRolesDbArray = connection.createArrayOf("text", userRoles.toArray());
      Array searchFieldsDbArray = connection.createArrayOf("text", fields.toArray());
      statement.setString(1, tableName);
      statement.setArray(2, userRolesDbArray);
      statement.setString(3, SEARCH_TYPE_OPERATION);
      statement.setArray(4, searchFieldsDbArray);

      ResultSet rs = statement.executeQuery();
      if (rs.next()) {
        return rs.getBoolean(1);
      }
    } catch (SQLException e) {
      throw SQLExceptionResolverUtil.getDetailedExceptionFromSql(e);
    }
    return false;
  }

  private List<String> getRequestedFields(Class<O> entityType) {
    JavaType type = objectMapper.getTypeFactory().constructType(entityType);
    BeanDescription beanDescription = objectMapper.getSerializationConfig().introspect(type);
    return beanDescription.findProperties().stream()
        .map(BeanPropertyDefinition::getName)
        .collect(Collectors.toList());
  }
}
