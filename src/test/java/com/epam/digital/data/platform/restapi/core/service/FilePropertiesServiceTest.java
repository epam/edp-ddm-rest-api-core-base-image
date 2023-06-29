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

package com.epam.digital.data.platform.restapi.core.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.restapi.core.config.FileProcessing;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityContains;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityEnum;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityNestedFile;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FilePropertiesServiceTest {

  FileProcessing fileProcessing;
  FilePropertiesService instance;

  @BeforeEach
  void beforeEach() {
    fileProcessing = new FileProcessing();
    fileProcessing.setAllowedPackages(List.of("com.epam.digital.data.platform.restapi.core.dto"));
    instance = new FilePropertiesService(fileProcessing);
  }

  @Test
  void shouldNotFoundPropertiesWhenBodyObjectDoesNotContainFileFields() {
    var fileProperties = instance.getFileProperties(new MockEntityContains());

    assertThat(fileProperties).isEmpty();
  }

  @Test
  void shouldFoundPropertiesWhenBodyObjectIsList() {
    var mockEntity = new MockEntityFile();
    mockEntity.setScanCopy(new File());
    mockEntity.setAnotherScanCopy(new File());
    mockEntity.setPhotos(List.of(new File(), new File()));
    mockEntity.setAnotherPhotos(Collections.emptyList());
    var list = List.of(mockEntity, mockEntity);

    var fileProperties = instance.getFileProperties(list);

    assertThat(fileProperties).hasSize(8);
  }

  @Test
  void shouldFindPropertiesWithFileType() {
    var mockEntity = new MockEntityFile();
    mockEntity.setScanCopy(new File());
    mockEntity.setAnotherScanCopy(new File());
    mockEntity.setPhotos(List.of(new File(), new File()));
    mockEntity.setAnotherPhotos(Collections.emptyList());

    var fileProperties = instance.getFileProperties(mockEntity);

    assertThat(fileProperties).hasSize(4);
  }

  @Test
  void shouldFindNestedPropertiesWithFileType() {
    var personFullName = "John";
    var testFile = new File();
    var mockEntityNestedFile = new MockEntityNestedFile();
    var objectWithFile = new MockEntity();
    objectWithFile.setPersonFullName(personFullName);
    objectWithFile.setConsentId(UUID.randomUUID());
    objectWithFile.setConsentDate(LocalDateTime.now());
    objectWithFile.setPassportScanCopy(testFile);
    mockEntityNestedFile.setFile(testFile);
    mockEntityNestedFile.setFiles(List.of(testFile));
    mockEntityNestedFile.setMockEntity(objectWithFile);
    mockEntityNestedFile.setMockEntityArray(new MockEntity[]{objectWithFile});
    mockEntityNestedFile.setMockEntityEnum(MockEntityEnum.M);

    var fileProperties = instance.getFileProperties(mockEntityNestedFile);

    assertThat(fileProperties).hasSize(4);
    assertThat(mockEntityNestedFile.getFile()).isNotNull();
    assertThat(mockEntityNestedFile.getFiles()).isNotNull();
    assertThat(mockEntityNestedFile.getMockEntity().getPassportScanCopy()).isNotNull();
    assertThat(mockEntityNestedFile.getMockEntity().getPersonFullName()).isEqualTo(
        personFullName);
    assertThat(mockEntityNestedFile.getMockEntityArray()[0].getPassportScanCopy()).isNotNull();
  }

  @Test
  void shouldSetToNullFileFields() {
    var name = "John";
    var mockEntity = new MockEntityFile();
    mockEntity.setSomeField(name);
    mockEntity.setScanCopy(new File());
    mockEntity.setAnotherScanCopy(new File());
    mockEntity.setPhotos(List.of(new File(), new File()));

    instance.resetFileProperties(mockEntity);

    assertThat(mockEntity.getScanCopy()).isNull();
    assertThat(mockEntity.getAnotherScanCopy()).isNull();
    assertThat(mockEntity.getPhotos()).isNull();
    assertThat(mockEntity.getSomeField()).isEqualTo(name);
  }

  @Test
  void shouldSetToNullNestedFileFields() {
    var personFullName = "John";
    var file = new File("1", "2");
    var file2 = new File("2", "4");
    var file3 = new File("3", "6");
    var file4 = new File("4", "8");
    var testMockEntity = new MockEntity();
    testMockEntity.setPassportScanCopy(file3);
    testMockEntity.setPersonFullName(personFullName);
    var testMockEntity2 = new MockEntity();
    testMockEntity2.setPassportScanCopy(file4);
    var bodyObject = new MockEntityNestedFile();
    bodyObject.setFile(file);
    bodyObject.setFiles(List.of(file2));
    bodyObject.setMockEntity(testMockEntity);
    bodyObject.setMockEntityEnum(MockEntityEnum.W);
    bodyObject.setMockEntityArray(new MockEntity[]{testMockEntity2});

    instance.resetFileProperties(bodyObject);
    assertThat(bodyObject.getFile()).isNull();
    assertThat(bodyObject.getFiles()).isNull();
    assertThat(bodyObject.getMockEntity().getPassportScanCopy()).isNull();
    assertThat(bodyObject.getMockEntity().getPersonFullName()).isEqualTo(personFullName);
    assertThat(bodyObject.getMockEntityArray()[0].getPassportScanCopy()).isNull();
    assertThat(bodyObject.getMockEntityEnum()).isEqualTo(MockEntityEnum.W);
  }
}