package com.epam.digital.data.platform.restapi.core.exception;

public class NoKafkaResponseException extends RuntimeException {

  public NoKafkaResponseException(String message, Exception e) {
    super(message, e);
  }
}
