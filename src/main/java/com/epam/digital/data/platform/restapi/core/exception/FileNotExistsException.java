package com.epam.digital.data.platform.restapi.core.exception;

import java.util.List;

public class FileNotExistsException extends RuntimeException {

  private final List<String> fieldsWithNotExistsFiles;

  public FileNotExistsException(String message, List<String> fieldsWithNotExistsFiles) {
    super(message);
    this.fieldsWithNotExistsFiles = fieldsWithNotExistsFiles;
  }

  @Override
  public String getMessage() {
    return super.getMessage() + ": " + fieldsWithNotExistsFiles;
  }

  public List<String> getFieldsWithNotExistsFiles() {
    return fieldsWithNotExistsFiles;
  }
}
