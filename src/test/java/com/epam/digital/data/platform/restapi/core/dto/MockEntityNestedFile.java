/*
 * Copyright 2023 EPAM Systems.
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

public class MockEntityNestedFile {

  private File file;
  private List<File> files;
  private MockEntity mockEntity;
  private MockEntity[] mockEntityArray;
  private MockEntityEnum mockEntityEnum;

  public File getFile() {
    return file;
  }

  public void setFile(File file) {
    this.file = file;
  }

  public List<File> getFiles() {
    return files;
  }

  public void setFiles(List<File> files) {
    this.files = files;
  }

  public MockEntity getMockEntity() {
    return mockEntity;
  }

  public void setMockEntity(MockEntity mockEntity) {
    this.mockEntity = mockEntity;
  }

  public MockEntity[] getMockEntityArray() {
    return mockEntityArray;
  }

  public void setMockEntityArray(
      MockEntity[] mockEntityArray) {
    this.mockEntityArray = mockEntityArray;
  }

  public MockEntityEnum getMockEntityEnum() {
    return mockEntityEnum;
  }

  public void setMockEntityEnum(MockEntityEnum mockEntityEnum) {
    this.mockEntityEnum = mockEntityEnum;
  }
}
