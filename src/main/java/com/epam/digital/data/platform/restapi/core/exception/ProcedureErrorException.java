package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Status;

public class ProcedureErrorException extends RequestProcessingException {
  public ProcedureErrorException(String message) {
    super(message, Status.PROCEDURE_ERROR);
  }

  public ProcedureErrorException(String message, Throwable cause) {
    super(message, cause, Status.PROCEDURE_ERROR);
  }
}
