package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Status;

public class SqlErrorException extends RequestProcessingException {

  public SqlErrorException(String message, Throwable cause) {
    super(message, cause, Status.SQL_ERROR);
  }
}
