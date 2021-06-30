package com.epam.digital.data.platform.restapi.core.impl.model;

public class TestEntitySearchConditions {

  private TypGender personGender;
  private String personFullName;
  private Integer offset;
  private Integer limit;

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

  public Integer getOffset() {
    return offset;
  }

  public void setOffset(Integer offset) {
    this.offset = offset;
  }

  public Integer getLimit() {
    return limit;
  }

  public void setLimit(Integer limit) {
    this.limit = limit;
  }
}
