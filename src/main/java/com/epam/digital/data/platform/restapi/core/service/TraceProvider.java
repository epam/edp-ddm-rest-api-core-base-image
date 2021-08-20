package com.epam.digital.data.platform.restapi.core.service;

public interface TraceProvider {

  String getRequestId();

  String getAccessToken();
}
