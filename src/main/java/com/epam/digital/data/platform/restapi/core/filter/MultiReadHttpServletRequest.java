package com.epam.digital.data.platform.restapi.core.filter;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

public class MultiReadHttpServletRequest extends HttpServletRequestWrapper {

  private String body;

  public MultiReadHttpServletRequest(HttpServletRequest request) throws IOException {
    super(request);
    body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
  }

  @Override
  public ServletInputStream getInputStream() {
    return new CustomServletInputStream(body.getBytes());
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(this.getInputStream()));
  }
}
