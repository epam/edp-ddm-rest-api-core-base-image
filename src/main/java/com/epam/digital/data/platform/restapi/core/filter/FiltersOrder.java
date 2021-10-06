package com.epam.digital.data.platform.restapi.core.filter;

public class FiltersOrder {

  public static final int LOGBOOK_FILTER = 0;
  public static final int FILTER_CHAIN_EXCEPTION_HANDLER = LOGBOOK_FILTER + 1;
  public static final int HEADER_VALIDATION_FILTER = FILTER_CHAIN_EXCEPTION_HANDLER + 1;
  public static final int DIGITAL_SIGNATURE_VALIDATION_FILTER = HEADER_VALIDATION_FILTER + 1;

  private FiltersOrder() {
  }
}
