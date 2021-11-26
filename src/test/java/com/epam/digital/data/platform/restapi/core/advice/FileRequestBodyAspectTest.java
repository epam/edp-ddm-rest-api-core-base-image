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

package com.epam.digital.data.platform.restapi.core.advice;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import com.epam.digital.data.platform.restapi.core.exception.FileNotExistsException;
import com.epam.digital.data.platform.restapi.core.model.FileProperty;
import com.epam.digital.data.platform.restapi.core.service.FileService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileRequestBodyAspectTest {

  static final String FILE_PROPERTY_NAME = "scanCopy";
  static final String ANOTHER_FILE_PROPERTY_NAME = "anotherScanCopy";
  static final String FILE_ID = "id";

  @Mock
  FileService fileService;

  FileRequestBodyAspect instance;

  File scanCopy = new File();
  File anotherScanCopy = new File();
  MockEntityFile mockFile = new MockEntityFile();

  RequestContext requestContext = new RequestContext();

  @BeforeEach
  void beforeEach() {
    instance = new FileRequestBodyAspect(fileService);

    scanCopy.setId(FILE_ID);
    mockFile.setScanCopy(scanCopy);
    mockFile.setAnotherScanCopy(anotherScanCopy);

    requestContext.setBusinessProcessInstanceId("my source bp instance id");

    when(fileService.getFileProperties(any())).thenReturn(List.of(
        new FileProperty(FILE_PROPERTY_NAME, scanCopy),
        new FileProperty(ANOTHER_FILE_PROPERTY_NAME, anotherScanCopy)
    ));
  }

  @Test
  void expectProcessFileIfFileFieldsExists() {
    when(fileService.store(any(), any())).thenReturn(true);

    instance.process(null, mockFile, requestContext, null);

    verify(fileService, times(2)).store(any(), any());
  }

  @Test
  void expectFileNotExistsExceptionIfFileNotExistsInCeph() {
    when(fileService.store(any(), any())).thenReturn(false);

    var ex = assertThrows(FileNotExistsException.class, () -> instance
        .process(null, mockFile, requestContext, null));

    assertThat(ex.getFieldsWithNotExistsFiles())
        .containsExactlyInAnyOrder(FILE_PROPERTY_NAME, ANOTHER_FILE_PROPERTY_NAME);
  }

  @Test
  void expectSkipFlowIfNoFileFields() {
    when(fileService.getFileProperties(any())).thenReturn(emptyList());

    instance.process(null, new MockEntity(), requestContext, null);

    verify(fileService).getFileProperties(any());
    verifyNoMoreInteractions(fileService);
  }
}
