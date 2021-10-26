package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.model.core.kafka.Status;

public class RequestProcessingException extends RuntimeException {

  private final Status kafkaResponseStatus;
  private final String details;

  public RequestProcessingException(String message, Status kafkaResponseStatus) {
    this(message, null, kafkaResponseStatus, null);
  }

  public RequestProcessingException(String message, Throwable cause, Status kafkaResponseStatus) {
    this(message, cause, kafkaResponseStatus, null);
  }

  public RequestProcessingException(String message, Status kafkaResponseStatus, String details) {
    this(message, null, kafkaResponseStatus, details);
  }

  public RequestProcessingException(String message, Throwable cause, Status kafkaResponseStatus, String details) {
    super(message, cause);
    this.kafkaResponseStatus = kafkaResponseStatus;
    this.details = details;
  }

  public Status getKafkaResponseStatus() {
    return kafkaResponseStatus;
  }

  public String getDetails() {
    return details;
  }
}
