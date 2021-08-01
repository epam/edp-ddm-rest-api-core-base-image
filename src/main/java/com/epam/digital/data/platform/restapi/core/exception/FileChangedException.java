package com.epam.digital.data.platform.restapi.core.exception;

public class FileChangedException extends RuntimeException {
  public FileChangedException(String message) {
    super(message);
  }
}
