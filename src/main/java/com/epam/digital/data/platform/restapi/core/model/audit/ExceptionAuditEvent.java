package com.epam.digital.data.platform.restapi.core.model.audit;

import com.epam.digital.data.platform.starter.audit.model.EventType;

public class ExceptionAuditEvent {
  private EventType eventType;
  private String action;
  private boolean userInfoEnabled;

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(EventType eventType) {
    this.eventType = eventType;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public boolean isUserInfoEnabled() {
    return userInfoEnabled;
  }

  public void setUserInfoEnabled(boolean userInfoEnabled) {
    this.userInfoEnabled = userInfoEnabled;
  }
}
