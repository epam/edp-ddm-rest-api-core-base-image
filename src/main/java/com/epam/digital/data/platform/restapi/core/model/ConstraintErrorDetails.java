package com.epam.digital.data.platform.restapi.core.model;

public class ConstraintErrorDetails {

  private final String constraint;

  public ConstraintErrorDetails(String constraint) {
    this.constraint = constraint;
  }

  public String getConstraint() {
    return constraint;
  }
}
