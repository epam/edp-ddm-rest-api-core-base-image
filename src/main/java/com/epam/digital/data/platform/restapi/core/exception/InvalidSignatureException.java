package com.epam.digital.data.platform.restapi.core.exception;

public class InvalidSignatureException extends RuntimeException {

  public InvalidSignatureException(String msg) {
    super(msg);
  }
}
