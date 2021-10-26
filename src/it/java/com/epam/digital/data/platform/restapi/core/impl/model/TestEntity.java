package com.epam.digital.data.platform.restapi.core.impl.model;

import com.epam.digital.data.platform.model.core.xmladapter.LocalDateTimeXmlAdapter;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.validation.constraints.Pattern;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

public class TestEntity {

  private UUID id;
  @XmlJavaTypeAdapter(LocalDateTimeXmlAdapter.class)
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
  private LocalDateTime consentDate;
  @Pattern(regexp = "^[АВЕІКМНОРСТХ]{2}[0-9]{6}$")
  private String personPassNumber;
  private String personFullName;
  private TypGender personGender;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public LocalDateTime getConsentDate() {
    return consentDate;
  }

  public void setConsentDate(LocalDateTime consentDate) {
    this.consentDate = consentDate;
  }

  public String getPersonPassNumber() {
    return personPassNumber;
  }

  public void setPersonPassNumber(String personPassNumber) {
    this.personPassNumber = personPassNumber;
  }

  public String getPersonFullName() {
    return personFullName;
  }

  public void setPersonFullName(String personFullName) {
    this.personFullName = personFullName;
  }

  public TypGender getPersonGender() {
    return personGender;
  }

  public void setPersonGender(
      TypGender personGender) {
    this.personGender = personGender;
  }
}
