package com.epam.digital.data.platform.restapi.core.filter.validation;

import com.epam.digital.data.platform.restapi.core.exception.InvalidFormatHeaderValidationException;
import com.epam.digital.data.platform.restapi.core.utils.Header;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class UuidFormatHeaderValidatorTest {

  static final String TYPICAL_UUID = "123e4567-e89b-12d3-a456-426655440000";

  UuidFormatHeaderValidator instance = new UuidFormatHeaderValidator();

  @Test
  void doNothingWhenUUID() {
    instance.validate(Header.X_SOURCE_BUSINESS_ACTIVITY_INSTANCE_ID, TYPICAL_UUID);
  }

  @Test
  void throwExceptionWhenNotUUID() {
    Assertions.assertThatThrownBy(
            () -> instance.validate(Header.X_SOURCE_BUSINESS_ACTIVITY_INSTANCE_ID, "not a UUID"))
        .isInstanceOf(InvalidFormatHeaderValidationException.class);
  }
}
