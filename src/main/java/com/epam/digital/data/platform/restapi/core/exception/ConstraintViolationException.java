package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Status;

public class ConstraintViolationException extends RequestProcessingException {
  public ConstraintViolationException(String message, String details) {
    super(message, Status.CONSTRAINT_VIOLATION, details);
  }

  public ConstraintViolationException(String message, Throwable cause, String details) {
    super(message, cause, Status.CONSTRAINT_VIOLATION, details);
  }
}
