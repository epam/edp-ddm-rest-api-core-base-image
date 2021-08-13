package com.epam.digital.data.platform.restapi.core.filter.validation;

import com.epam.digital.data.platform.restapi.core.exception.InvalidFormatHeaderValidationException;
import com.epam.digital.data.platform.restapi.core.filter.validation.HeaderValidator;
import com.epam.digital.data.platform.restapi.core.utils.Header;
import java.util.UUID;

public class UuidFormatHeaderValidator implements HeaderValidator {

  @Override
  public void validate(Header header, String value) {
    try {
      UUID.fromString(value);
    } catch (Exception e) {
      throw new InvalidFormatHeaderValidationException(
          header,
          String.format("UUID expected in: '%s'", value));
    }
  }
}
