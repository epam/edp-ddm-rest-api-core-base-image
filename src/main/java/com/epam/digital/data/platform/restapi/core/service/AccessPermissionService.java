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

package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.restapi.core.model.FieldsAccessCheckDto;
import com.epam.digital.data.platform.restapi.core.utils.JwtClaimsUtils;
import com.epam.digital.data.platform.restapi.core.utils.SQLExceptionResolverUtil;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Component
public class AccessPermissionService {

  private static final String PERMISSION_CHECK_SQL_STRING =
      "select f_check_permissions(?, ?, ?::type_operation, ?);";
  private static final String SEARCH_TYPE_OPERATION = "S";

  private final DataSource dataSource;

  public AccessPermissionService(
      DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public boolean hasReadAccess(
      List<FieldsAccessCheckDto> accessedFieldsDto, JwtClaimsDto userClaims) {
    List<String> userRoles = JwtClaimsUtils.getRoles(userClaims);
    try {
      Connection connection = dataSource.getConnection();
      CallableStatement statement = connection.prepareCall(PERMISSION_CHECK_SQL_STRING);
      Array userRolesDbArray = connection.createArrayOf("text", userRoles.toArray());
      for (FieldsAccessCheckDto tableFields : accessedFieldsDto) {
        Array searchFieldsDbArray = connection.createArrayOf("text", tableFields.getFields().toArray());
        statement.setString(1, tableFields.getTableName());
        statement.setArray(2, userRolesDbArray);
        statement.setString(3, SEARCH_TYPE_OPERATION);
        statement.setArray(4, searchFieldsDbArray);

        ResultSet rs = statement.executeQuery();
        if (rs.next()) {
          boolean hasTableAccess = rs.getBoolean(1);
          if (!hasTableAccess) {
            return false;
          }
        } else {
          return false;
        }
      }
    } catch (SQLException e) {
      throw SQLExceptionResolverUtil.getDetailedExceptionFromSql(e);
    }
    return true;
  }
}
