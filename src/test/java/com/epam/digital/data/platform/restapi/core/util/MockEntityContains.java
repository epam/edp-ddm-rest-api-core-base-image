package com.epam.digital.data.platform.restapi.core.util;

import com.fasterxml.jackson.annotation.JsonFormat;

import javax.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.UUID;

public class MockEntityContains {

  private UUID consentId;

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private LocalDateTime consentDate;

  private String personFullName;

  @Pattern(regexp = "^[АВЕІКМНОРСТХ]{2}[0-9]{6}$")
  private String personPassNumber;

  public UUID getConsentId() {
    return this.consentId;
  }

  public void setConsentId(UUID consentId) {
    this.consentId = consentId;
  }

  public LocalDateTime getConsentDate() {
    return this.consentDate;
  }

  public void setConsentDate(LocalDateTime consentDate) {
    this.consentDate = consentDate;
  }

  public String getPersonFullName() {
    return this.personFullName;
  }

  public void setPersonFullName(String personFullName) {
    this.personFullName = personFullName;
  }

  public String getPersonPassNumber() {
    return this.personPassNumber;
  }

  public void setPersonPassNumber(String personPassNumber) {
    this.personPassNumber = personPassNumber;
  }
}
