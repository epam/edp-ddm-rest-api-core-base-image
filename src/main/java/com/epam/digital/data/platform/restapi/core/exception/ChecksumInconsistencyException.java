package com.epam.digital.data.platform.restapi.core.exception;

public class ChecksumInconsistencyException extends RuntimeException {
  public ChecksumInconsistencyException(String message) {
    super(message);
  }
}
