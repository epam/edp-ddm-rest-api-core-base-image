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

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import com.epam.digital.data.platform.restapi.core.exception.NotFoundException;
import com.epam.digital.data.platform.restapi.core.model.FileRequestDto;
import com.epam.digital.data.platform.restapi.core.queryhandler.impl.QueryHandlerFileTestImpl;
import com.epam.digital.data.platform.restapi.core.service.impl.GenericFileQueryServiceTestImpl;
import com.epam.digital.data.platform.storage.file.dto.FileDataDto;
import com.epam.digital.data.platform.storage.file.dto.FileMetadataDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = GenericFileQueryServiceTestImpl.class)
class GenericFileQueryServiceTest {

  private static final UUID ENTITY_ID = UUID.fromString("123e4567-e89b-12d3-a456-426655440000");
  private static final String FILE_ID = "fileId";

  @MockBean
  private QueryHandlerFileTestImpl mockQueryHandler;
  @MockBean
  private FileService fileService;

  @Autowired
  private GenericFileQueryServiceTestImpl instance;

  @Test
  void expectNotFoundWhenNoEntityInDbOnRequestDto() {
    when(mockQueryHandler.findById(any())).thenReturn(Optional.empty());

    var actual = instance.requestDto(mockInput());

    verify(fileService, never()).retrieve(any());
    assertThat(actual.getPayload()).isNull();
    assertThat(actual.getStatus()).isEqualTo(Status.NOT_FOUND);
  }

  @Test
  void expectEntityReturnedWhenFoundInDbForRequestDto() {
    var mockEntityFile = new MockEntityFile();
    var checksum = "checksum";
    var file = new File(FILE_ID, checksum);
    mockEntityFile.setScanCopy(file);
    var fileContent = "content".getBytes(StandardCharsets.UTF_8);
    var filename = "filename";
    when(mockQueryHandler.findById(any())).thenReturn(Optional.of(mockEntityFile));
    when(fileService.retrieve(file))
        .thenReturn(
            FileDataDto.builder()
                .content(new ByteArrayInputStream(fileContent))
                .metadata(FileMetadataDto.builder().filename(filename).build())
                .build());

    var actual = instance.requestDto(mockInput());

    assertThat(actual.getPayload().getContent()).isEqualTo(Base64.getEncoder().encodeToString(fileContent));
    assertThat(actual.getPayload().getChecksum()).isEqualTo(checksum);
    assertThat(actual.getPayload().getFilename()).isEqualTo(filename);
    assertThat(actual.getStatus()).isEqualTo(Status.SUCCESS);
  }

  @Test
  void expectNotFoundExceptionWhenNoEntityInDbOnRequestFile() {
    when(mockQueryHandler.findById(any())).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> instance.requestFile(mockInput()));

    verify(fileService, never()).retrieve(any());
  }

  @Test
  void expectEntityReturnedWhenFoundInDbForRequestFile() {
    var mockEntityFile = new MockEntityFile();
    var checksum = "checksum";
    var file = new File(FILE_ID, checksum);
    mockEntityFile.setScanCopy(file);
    var fileContent = "content".getBytes(StandardCharsets.UTF_8);
    var filename = "filename";
    var fileDataDto = FileDataDto.builder()
            .content(new ByteArrayInputStream(fileContent))
            .metadata(FileMetadataDto.builder().filename(filename).build())
            .build();
    when(mockQueryHandler.findById(any())).thenReturn(Optional.of(mockEntityFile));
    when(fileService.retrieve(file)).thenReturn(fileDataDto);

    var actual = instance.requestFile(mockInput());

    assertThat(actual.getPayload()).isEqualTo(fileDataDto);
    assertThat(actual.getStatus()).isEqualTo(Status.SUCCESS);
  }
  private Request<FileRequestDto<UUID>> mockInput() {
    return new Request<>(new FileRequestDto<>(ENTITY_ID, FILE_ID), null, null);
  }
}
