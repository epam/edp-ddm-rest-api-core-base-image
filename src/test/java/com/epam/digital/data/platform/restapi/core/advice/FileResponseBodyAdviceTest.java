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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import com.epam.digital.data.platform.restapi.core.exception.FileNotExistsException;
import com.epam.digital.data.platform.restapi.core.exception.MandatoryHeaderMissingException;
import com.epam.digital.data.platform.restapi.core.model.FileProperty;
import com.epam.digital.data.platform.restapi.core.service.FileService;
import com.epam.digital.data.platform.restapi.core.utils.Header;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;

@ExtendWith(MockitoExtension.class)
class FileResponseBodyAdviceTest {

  static final String FILE_PROPERTY_NAME = "scanCopy";
  static final String ANOTHER_FILE_PROPERTY_NAME = "anotherScanCopy";
  static final String FILE_ID = "id";

  FileResponseBodyAdvice instance;

  @Mock
  FileService fileService;

  @Mock
  ServerHttpRequest req;

  File scanCopy = new File();
  File anotherScanCopy = new File();
  MockEntityFile mockFile = new MockEntityFile();

  @BeforeEach
  void beforeEach() {
    instance = new FileResponseBodyAdvice(fileService);

    scanCopy.setId(FILE_ID);
    mockFile.setScanCopy(scanCopy);
    mockFile.setAnotherScanCopy(anotherScanCopy);
  }

  @Test
  void expectProcessFileIfFileFieldsExists() {
    setupFileProperties();
    setupInstanceIdHeader();
    when(fileService.retrieve(any(), any())).thenReturn(true);

    instance.beforeBodyWrite(mockFile, null, null, null, req, null);

    verify(fileService, times(2)).retrieve(any(), any());
  }

  @Test
  void expectFileNotExistsExceptionIfFileNotExistsInCeph() {
    setupFileProperties();
    setupInstanceIdHeader();
    when(fileService.retrieve(any(), any())).thenReturn(false);

    var ex = assertThrows(FileNotExistsException.class, () -> instance
        .beforeBodyWrite(mockFile, null, null, null, req, null));

    assertThat(ex.getFieldsWithNotExistsFiles())
        .containsExactlyInAnyOrder(FILE_PROPERTY_NAME, ANOTHER_FILE_PROPERTY_NAME);
  }

  @Test
  void expectSkipFlowIfNoFileFields() {
    setupFileProperties();
    when(fileService.getFileProperties(any())).thenReturn(emptyList());

    instance.beforeBodyWrite(new MockEntity(), null, null, null, req, null);

    verify(fileService).getFileProperties(any());
    verifyNoMoreInteractions(fileService);
  }

  @Test
  void expectNullIfNullGiven() {

    var o = instance.beforeBodyWrite(null, null, null, null, req, null);

    verifyNoInteractions(fileService);
    assertThat(o).isNull();
  }

  @Test
  void failIfInstanceIdHeaderMissed() {
    setupFileProperties();
    when(req.getHeaders()).thenReturn(new HttpHeaders());

    assertThrows(MandatoryHeaderMissingException.class, () -> instance
        .beforeBodyWrite(mockFile, null, null, null, req, null));
  }

  private void setupFileProperties() {
    when(fileService.getFileProperties(any())).thenReturn(List.of(
        new FileProperty(FILE_PROPERTY_NAME, scanCopy),
        new FileProperty(ANOTHER_FILE_PROPERTY_NAME, anotherScanCopy)
    ));
  }

  private void setupInstanceIdHeader() {
    var headers = new HttpHeaders();
    headers.add(Header.X_SOURCE_BUSINESS_PROCESS_INSTANCE_ID.getHeaderName(), "instanceId");
    when(req.getHeaders()).thenReturn(headers);
  }
}
