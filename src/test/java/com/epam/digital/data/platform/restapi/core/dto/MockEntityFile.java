/*
 * Copyright 2021 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.restapi.core.dto;

import com.epam.digital.data.platform.model.core.kafka.File;
import java.util.List;
import java.util.UUID;
import javax.validation.constraints.Size;

public class MockEntityFile {

  public static final int FILE_FIELD_NUM = 2;

  private UUID id;
  @Size(max = 5)
  private String someField;
  private File scanCopy;
  private File anotherScanCopy;
  private List<File> photos;
  private List<File> anotherPhotos;

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

  public List<File> getPhotos() {
    return photos;
  }

  public void setPhotos(List<File> photos) {
    this.photos = photos;
  }

  public List<File> getAnotherPhotos() {
    return anotherPhotos;
  }

  public void setAnotherPhotos(
      List<File> anotherPhotos) {
    this.anotherPhotos = anotherPhotos;
  }
}
