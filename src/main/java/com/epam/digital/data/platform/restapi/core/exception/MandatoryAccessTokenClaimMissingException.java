package com.epam.digital.data.platform.restapi.core.exception;

import java.util.List;

public class MandatoryAccessTokenClaimMissingException extends RuntimeException {

  private final List<String> missed;

  public MandatoryAccessTokenClaimMissingException(List<String> missed) {
    super("Mandatory access token claim(s) missed");
    this.missed = missed;
  }

  @Override
  public String getMessage() {
    return super.getMessage() + ": " + missed;
  }
}
