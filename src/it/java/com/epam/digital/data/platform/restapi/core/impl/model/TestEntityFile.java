package com.epam.digital.data.platform.restapi.core.impl.model;

import com.epam.digital.data.platform.model.core.kafka.File;
import java.util.UUID;

public class TestEntityFile {

  private UUID id;
  private String legalEntityName;
  private File scanCopy;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getLegalEntityName() {
    return legalEntityName;
  }

  public void setLegalEntityName(String legalEntityName) {
    this.legalEntityName = legalEntityName;
  }

  public File getScanCopy() {
    return scanCopy;
  }

  public void setScanCopy(File scanCopy) {
    this.scanCopy = scanCopy;
  }
}
