package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Response;
import org.springframework.http.HttpStatus;

public class KafkaSecurityValidationFailedException extends KafkaInvalidResponseException {
  public KafkaSecurityValidationFailedException(
      String message, Response<?> kafkaResponse, HttpStatus httpStatus) {
    super(message, kafkaResponse, httpStatus);
  }
}
