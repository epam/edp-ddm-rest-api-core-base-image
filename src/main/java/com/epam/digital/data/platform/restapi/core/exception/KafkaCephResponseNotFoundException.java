package com.epam.digital.data.platform.restapi.core.exception;

public class KafkaCephResponseNotFoundException extends RuntimeException {
  public KafkaCephResponseNotFoundException(String message) {
    super(message);
  }
}
