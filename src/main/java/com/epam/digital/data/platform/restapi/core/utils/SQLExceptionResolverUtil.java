package com.epam.digital.data.platform.restapi.core.utils;

import com.epam.digital.data.platform.restapi.core.exception.ConstraintViolationException;
import com.epam.digital.data.platform.restapi.core.exception.ForbiddenOperationException;
import com.epam.digital.data.platform.restapi.core.exception.ProcedureErrorException;
import com.epam.digital.data.platform.restapi.core.exception.RequestProcessingException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

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
    boolean isConstraintViolation = sqlErrorCodeToDetail.containsKey(sqlState);
    if (isConstraintViolation) {
      return new ConstraintViolationException(
          "Constraint violation occured", exception, sqlErrorCodeToDetail.get(sqlState));
    } else if (PERMISSION_DENIED_ERROR_CODE.equals(sqlState)) {
      return new ForbiddenOperationException("User has invalid role for this operation");
    } else {
      return new ProcedureErrorException("Procedure error on DB call occured", exception);
    }
  }
}
