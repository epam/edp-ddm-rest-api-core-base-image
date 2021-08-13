package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.restapi.core.utils.Header;

public abstract class HeaderValidationException extends RuntimeException {

  private final Header header;
  private final String msg;

  protected HeaderValidationException(Header header, String msg) {
    super(msg);
    this.header = header;
    this.msg = msg;
  }

  @Override
  public String getMessage() {
    return header.getHeaderName() + ": " + super.getMessage();
  }
}
