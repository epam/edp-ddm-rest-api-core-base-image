package com.epam.digital.data.platform.restapi.core.exception;

import com.epam.digital.data.platform.restapi.core.utils.Header;

public class InvalidFormatHeaderValidationException extends HeaderValidationException {

  public InvalidFormatHeaderValidationException(Header header, String msg) {
    super(header, msg);
  }
}
