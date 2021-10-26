package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Status;

public class ForbiddenOperationException extends RequestProcessingException {

  public ForbiddenOperationException(String message) {
    super(message, Status.FORBIDDEN_OPERATION);
  }
}
