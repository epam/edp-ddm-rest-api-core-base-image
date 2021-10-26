package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Status;

public class JwtValidationException extends RequestProcessingException {

  public JwtValidationException(String message, Throwable cause) {
    super(message, cause, Status.JWT_INVALID);
  }

  public JwtValidationException(String message) {
    super(message, Status.JWT_INVALID);
  }
}
