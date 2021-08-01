package com.epam.digital.data.platform.restapi.core.filter;

class FiltersOrder {

  static final int filterChainExceptionHandler = 0;
  static final int headerValidationFilter = filterChainExceptionHandler + 1;
  static final int digitalSignatureValidationFilter = headerValidationFilter + 1;

  private FiltersOrder() {
  }
}
