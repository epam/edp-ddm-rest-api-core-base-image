package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Response;
import org.springframework.http.HttpStatus;

public class KafkaInvalidResponseException extends RuntimeException {

  private final Response<?> kafkaResponse;
  private final HttpStatus httpStatus;

  public KafkaInvalidResponseException(String message, Response<?> kafkaResponse,
      HttpStatus httpStatus) {
    super(message);
    this.kafkaResponse = kafkaResponse;
    this.httpStatus = httpStatus;
  }

  public Response<?> getKafkaResponse() {
    return kafkaResponse;
  }

  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}
