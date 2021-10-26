package com.epam.digital.data.platform.restapi.core.exception;

public class CreateKafkaTopicException extends RuntimeException {

  public CreateKafkaTopicException(String message, Exception e) {
    super(message, e);
  }
}
