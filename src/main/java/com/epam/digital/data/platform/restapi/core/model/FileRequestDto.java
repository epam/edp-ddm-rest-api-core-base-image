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

package com.epam.digital.data.platform.restapi.core.model;

public class FileRequestDto<T> {

  private T entityId;
  private String fileId;

  public FileRequestDto() {
  }

  public FileRequestDto(T entityId, String fileId) {
    this.entityId = entityId;
    this.fileId = fileId;
  }

  public T getEntityId() {
    return entityId;
  }

  public void setEntityId(T entityId) {
    this.entityId = entityId;
  }

  public String getFileId() {
    return fileId;
  }

  public void setFileId(String fileId) {
    this.fileId = fileId;
  }
}
