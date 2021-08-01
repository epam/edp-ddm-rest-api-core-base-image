package com.epam.digital.data.platform.restapi.core.model;

import com.epam.digital.data.platform.model.core.kafka.File;

public class FileProperty {

  private final String name;
  private final File value;

  public FileProperty(String name, File value) {
    this.name = name;
    this.value = value;
  }

  public String getName() {
    return name;
  }

  public File getValue() {
    return value;
  }
}
