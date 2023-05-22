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

import static com.epam.digital.data.platform.restapi.core.dto.MockEntityFile.FILE_FIELD_NUM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.epam.digital.data.platform.integration.ceph.exception.CephCommunicationException;
import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.restapi.core.config.FileProcessing;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityEnum;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityNestedFile;
import com.epam.digital.data.platform.restapi.core.exception.ChecksumInconsistencyException;
import com.epam.digital.data.platform.restapi.core.utils.ResponseCode;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import com.epam.digital.data.platform.storage.file.dto.FileMetadataDto;
import com.epam.digital.data.platform.storage.file.exception.FileNotFoundException;
import com.epam.digital.data.platform.storage.file.service.FormDataFileKeyProvider;
import com.epam.digital.data.platform.storage.file.service.FormDataFileKeyProviderImpl;
import com.epam.digital.data.platform.storage.file.service.FormDataFileStorageService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {


  static final String FILE_ID = "id";
  static final String INSTANCE_ID = "instanceId";
  static final String COMPOSITE_FILE_ID = "process/" + INSTANCE_ID + "/" + FILE_ID;

  static final byte[] FILE_CONTENT = "content".getBytes();
  static final String HASH_OF_CONTENT_STRING =
          "ed7002b439e9ac845f22357d822bac1444730fbdb6016d3ec9432297b9ec9f73";

  static final String FILE_CONTENT_TYPE = "image/jpeg";

  @Mock
  FormDataFileStorageService lowcodeCephService;
  @Mock
  FormDataFileStorageService datafactoryCephService;

  FormDataFileKeyProvider fileKeyProvider = new FormDataFileKeyProviderImpl();
  FileProcessing fileProcessing = new FileProcessing();

  FileService instance;

  @BeforeEach
  void beforeEach() {
    fileProcessing.setEnabled(true);
    fileProcessing.setAllowedPackages(List.of("com.epam.digital.data.platform.restapi.core.dto"));
    instance =
            new FileService(
                    lowcodeCephService,
                    datafactoryCephService, fileKeyProvider, fileProcessing);
  }

  private File mockFile(String compositeFileId) {
    var file = new File();
    file.setId(compositeFileId);
    file.setChecksum(HASH_OF_CONTENT_STRING);
    return file;
  }

  private FileDataDto createMockCephResponse(InputStream content) {
    return FileDataDto.builder()
            .content(content)
            .metadata(FileMetadataDto.builder()
                    .contentType(FILE_CONTENT_TYPE)
                    .filename("filename").build())
            .build();
  }

  @Nested
  class GetFileProperties {

    @Nested
    class ProcessingFieldsFile {

      @Test
      void skipNulls() {
        assertThat(instance.getFileProperties(new MockEntityFile())).isEmpty();
      }

      @Test
      void findPropertiesWithFileType() {
        MockEntityFile e = mockEntityFile();

        var fileProperties = instance.getFileProperties(e);

        assertThat(fileProperties).hasSize(FILE_FIELD_NUM);
      }

      @Test
      void findNestedPropertiesWithFileType() {
        var testFile = new File();
        var mockEntityNestedFile = new MockEntityNestedFile();
        var objectWithFile = new MockEntity();
        objectWithFile.setPersonFullName("testName");
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
      }

      @Test
      void dealWithCollections() {
        var list = List.of(mockEntityFile(), mockEntityFile());

        var fileProperties = instance.getFileProperties(list);

        assertThat(fileProperties).hasSize(list.size() * FILE_FIELD_NUM);
      }

      private MockEntityFile mockEntityFile() {
        var e = new MockEntityFile();
        e.setScanCopy(new File());
        e.setAnotherScanCopy(new File());
        return e;
      }
    }

    @Nested
    class ProcessingFieldsListOfFiles {

      @Test
      void skipEmptyLists() {
        var entity = mockEntityFile();
        entity.setAnotherPhotos(Collections.EMPTY_LIST);

        assertThat(instance.getFileProperties(entity)).hasSize(4);
      }

      @Test
      void dealWithCollections() {
        var list = List.of(mockEntityFile(), mockEntityFile());

        var fileProperties = instance.getFileProperties(list);

        assertThat(fileProperties).hasSize(14);
      }

      // 7 files
      private MockEntityFile mockEntityFile() {
        var e = new MockEntityFile();
        e.setScanCopy(new File());
        e.setAnotherScanCopy(new File());
        e.setPhotos(List.of(new File(), new File()));
        e.setAnotherPhotos(List.of(new File(), new File(), new File()));
        return e;
      }
    }
  }

  @Nested
  class Store {

    @Test
    void copyContentFromLowcodeBucketToDatafactoryBucket() throws IOException {
      InputStream fileContent = new ByteArrayInputStream(FILE_CONTENT);
      var fileDataDto = FileDataDto.builder()
              .content(fileContent)
              .build();
      when(lowcodeCephService.loadByKey(COMPOSITE_FILE_ID)).thenReturn(fileDataDto);


      File file = mockFile(FILE_ID);

      instance.store(INSTANCE_ID, file);

      var datafactoryContentCaptor = ArgumentCaptor.forClass(FileDataDto.class);
      verify(datafactoryCephService)
              .save(
                      eq(FILE_ID),
                      datafactoryContentCaptor.capture());
      assertThat(datafactoryContentCaptor.getValue().getContent().readAllBytes()).isEqualTo(FILE_CONTENT);

    }

    @Test
    void expectTrueWhenSuccess() {
      InputStream fileContent = new ByteArrayInputStream(FILE_CONTENT);
      var fileDataDto = FileDataDto.builder()
              .content(fileContent)
              .build();
      when(lowcodeCephService.loadByKey(COMPOSITE_FILE_ID)).thenReturn(fileDataDto);


      File file = mockFile(FILE_ID);

      var success = instance.store(INSTANCE_ID, file);

      assertThat(success).isTrue();
    }

    @Test
    void expectFalseIfFileNotExistsInCeph() {
      when(lowcodeCephService.loadByKey(COMPOSITE_FILE_ID))
              .thenThrow(FileNotFoundException.class);

      File file = mockFile(FILE_ID);

      var success = instance.store(INSTANCE_ID, file);

      assertThat(success).isFalse();
    }

    @Test
    void expectExceptionIfCephFailed() {
      var amazonS3Exception = new AmazonS3Exception("");
      amazonS3Exception.setErrorCode(ResponseCode.CLIENT_ERROR);
      amazonS3Exception.setStatusCode(INTERNAL_SERVER_ERROR.value());
      when(lowcodeCephService.loadByKey(COMPOSITE_FILE_ID))
              .thenThrow(new CephCommunicationException("", amazonS3Exception));

      File file = mockFile(FILE_ID);

      assertThrows(RuntimeException.class, () -> instance.store(INSTANCE_ID, file));
    }

    @Test
    void expectExceptionIfStoredFileWasChanged() {
      InputStream fileContent = new ByteArrayInputStream("wrong content".getBytes());
      var fileDataDto = FileDataDto.builder()
              .content(fileContent)
              .build();
      when(lowcodeCephService.loadByKey(COMPOSITE_FILE_ID))
              .thenReturn(fileDataDto);

      var file = mockFile(FILE_ID);

      assertThrows(ChecksumInconsistencyException.class, () -> instance.store(INSTANCE_ID, file));
    }

    @Test
    void expectSkipCephFlowIfFileProcessingDisabled() {
      fileProcessing.setEnabled(false);
      instance =
              new FileService(
                      lowcodeCephService,
                      datafactoryCephService, fileKeyProvider, fileProcessing);

      File file = mockFile(FILE_ID);

      instance.store(INSTANCE_ID, file);

      verifyNoInteractions(lowcodeCephService, datafactoryCephService);
    }
  }

  @Nested
  class Retrieve {

    @Test
    void copyContentFromDatafactoryBucketToLowcodeBucket() throws IOException {
      InputStream fileContent = new ByteArrayInputStream(FILE_CONTENT);
      when(datafactoryCephService.loadByKey(FILE_ID))
              .thenReturn(createMockCephResponse(fileContent));

      File file = mockFile(FILE_ID);

      instance.retrieve(INSTANCE_ID, file);

      var lowcodeContentCaptor = ArgumentCaptor.forClass(FileDataDto.class);
      verify(lowcodeCephService)
              .save(
                      eq(COMPOSITE_FILE_ID),
                      lowcodeContentCaptor.capture());
      assertThat(lowcodeContentCaptor.getValue().getContent().readAllBytes()).isEqualTo(FILE_CONTENT);

    }

    @Test
    void expectTrueWhenSuccess() {
      InputStream fileContent = new ByteArrayInputStream(FILE_CONTENT);
      when(datafactoryCephService.loadByKey(FILE_ID))
              .thenReturn(createMockCephResponse(fileContent));

      File file = mockFile(FILE_ID);

      var success = instance.retrieve(INSTANCE_ID, file);

      assertThat(success).isTrue();
    }

    @Test
    void expectFalseIfFileNotExistsInCeph() {
      when(datafactoryCephService.loadByKey(FILE_ID)).thenThrow(FileNotFoundException.class);
      File file = mockFile(FILE_ID);

      var success = instance.retrieve(INSTANCE_ID, file);

      assertThat(success).isFalse();
    }

    @Test
    void expectExceptionIfCephFailed() {
      var amazonS3Exception = new AmazonS3Exception("");
      amazonS3Exception.setErrorCode(ResponseCode.CLIENT_ERROR);
      amazonS3Exception.setStatusCode(INTERNAL_SERVER_ERROR.value());
      when(datafactoryCephService.loadByKey(FILE_ID))
              .thenThrow(new CephCommunicationException("", amazonS3Exception));

      File file = mockFile(FILE_ID);

      assertThrows(RuntimeException.class, () -> instance.retrieve(INSTANCE_ID, file));
    }

    @Test
    void ifRetrievedFileWasChangedWeReturnTrueButDontCopyToTargetBucket() {
      InputStream fileContent = new ByteArrayInputStream("wrong content".getBytes());
      when(datafactoryCephService.loadByKey(FILE_ID))
          .thenReturn(createMockCephResponse(fileContent));

      var file = mockFile(FILE_ID);

      var isFound = instance.retrieve(INSTANCE_ID, file);

      assertThat(isFound).isTrue();
      verify(lowcodeCephService, never()).save(any(), any());
    }

    @Test
    void expectSkipCephFlowIfFileProcessingDisabled() {
      fileProcessing.setEnabled(false);
      instance =
              new FileService(
                      lowcodeCephService,
                      datafactoryCephService, fileKeyProvider, fileProcessing);
      File file = mockFile(FILE_ID);

      instance.retrieve(INSTANCE_ID, file);

      verifyNoInteractions(lowcodeCephService, datafactoryCephService);
    }
  }
}
