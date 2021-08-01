package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Response;
import org.springframework.http.HttpStatus;

public class KafkaConstraintViolationException extends KafkaInvalidResponseException {

  public KafkaConstraintViolationException(String message, Response<?> kafkaResponse,
      HttpStatus httpStatus) {
    super(message, kafkaResponse, httpStatus);
  }
}