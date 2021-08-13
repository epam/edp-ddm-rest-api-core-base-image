package com.epam.digital.data.platform.restapi.core.filter.validation;

import com.epam.digital.data.platform.restapi.core.utils.Header;

public interface HeaderValidator {

  void validate(Header header, String value);
}
