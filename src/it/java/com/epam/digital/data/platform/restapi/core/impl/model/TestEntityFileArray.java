package com.epam.digital.data.platform.restapi.core.impl.model;

import com.epam.digital.data.platform.model.core.kafka.File;
import java.util.List;
import java.util.UUID;

public class TestEntityFileArray {

  private UUID id;
  private String legalEntityName;
  private List<File> scanCopies;

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

  public List<File> getScanCopies() {
    return scanCopies;
  }

  public void setScanCopies(List<File> scanCopies) {
    this.scanCopies = scanCopies;
  }
}
