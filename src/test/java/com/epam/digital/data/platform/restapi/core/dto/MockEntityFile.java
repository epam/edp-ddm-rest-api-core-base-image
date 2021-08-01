package com.epam.digital.data.platform.restapi.core.dto;

import com.epam.digital.data.platform.model.core.kafka.File;
import java.util.UUID;
import javax.validation.constraints.Size;

public class MockEntityFile {

  public static final int FILE_FIELD_NUM = 2;

  private UUID id;
  @Size(max = 5)
  private String someField;
  private File scanCopy;
  private File anotherScanCopy;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getSomeField() {
    return someField;
  }

  public void setSomeField(String someField) {
    this.someField = someField;
  }

  public File getScanCopy() {
    return scanCopy;
  }

  public void setScanCopy(File scanCopy) {
    this.scanCopy = scanCopy;
  }

  public File getAnotherScanCopy() {
    return anotherScanCopy;
  }

  public void setAnotherScanCopy(File anotherScanCopy) {
    this.anotherScanCopy = anotherScanCopy;
  }
}
