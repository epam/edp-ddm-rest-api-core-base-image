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

package com.epam.digital.data.platform.restapi.core.utils;

import com.epam.digital.data.platform.restapi.core.exception.ConstraintViolationException;
import com.epam.digital.data.platform.restapi.core.exception.ForbiddenOperationException;
import com.epam.digital.data.platform.restapi.core.exception.ProcedureErrorException;
import com.epam.digital.data.platform.restapi.core.exception.RequestProcessingException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class SQLExceptionResolverUtil {

  private static final String PERMISSION_DENIED_ERROR_CODE = "20003";

  private static final Map<String, String> sqlErrorCodeToDetail;

  static {
    sqlErrorCodeToDetail = new HashMap<>();
    sqlErrorCodeToDetail.put("23000", "integrity");
    sqlErrorCodeToDetail.put("23001", "restrict");
    sqlErrorCodeToDetail.put("23502", "not null");
    sqlErrorCodeToDetail.put("23503", "foreign key");
    sqlErrorCodeToDetail.put("23505", "unique");
    sqlErrorCodeToDetail.put("23514", "check");
    sqlErrorCodeToDetail.put("23P01", "exclusion");
  }

  private SQLExceptionResolverUtil() {}

  public static RequestProcessingException getDetailedExceptionFromSql(SQLException exception) {
    String sqlState = exception.getSQLState();
    String shortErrorDescription = Optional.ofNullable(exception.getMessage())
            .map(message -> message.split("\n")[0])
            .orElse("");
    boolean isConstraintViolation = sqlErrorCodeToDetail.containsKey(sqlState);
    if (isConstraintViolation) {
      return new ConstraintViolationException(
              "Constraint violation occurred: " + shortErrorDescription,
              exception,
              sqlErrorCodeToDetail.get(sqlState));
    } else if (PERMISSION_DENIED_ERROR_CODE.equals(sqlState)) {
      return new ForbiddenOperationException("User has invalid role for this operation");
    } else {
      return new ProcedureErrorException(
              "Procedure error on DB call occurred: " + shortErrorDescription, exception);
    }
  }
}