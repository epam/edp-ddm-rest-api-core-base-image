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

package com.epam.digital.data.platform.restapi.core.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.ReadListener;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomServletInputStreamTest {

  private CustomServletInputStream inputStream;

  @Mock
  private ReadListener readListener;

  @BeforeEach
  void init() {
    inputStream = new CustomServletInputStream("10charsStr");
  }

  @Test
  void shouldReturnMinusOneAfterTenAndMoreReads() throws IOException {
    readChars(9);
    assertTrue(inputStream.read() != -1);
    assertTrue(inputStream.read() == -1);
    assertTrue(inputStream.read() == -1);
  }

  @Test
  void shouldInvoke_OnDataAvailable_WhenDataIsAvailableToRead() throws IOException {
    inputStream.setReadListener(readListener);

    verify(readListener).onDataAvailable();
  }

  @Test
  void shouldInvoke_OnAllDataRead_WhenTheRequestBodyHasBeenFullyRead() throws IOException {
    readChars(10);

    inputStream.setReadListener(readListener);

    verify(readListener).onAllDataRead();
  }

  @Test
  void shouldInvoke_OnError_IfAnErrorOccursInMethod_OnDataAvailable() throws IOException {
    IOException ioException = new IOException();
    doThrow(ioException).when(readListener).onDataAvailable();

    inputStream.setReadListener(readListener);

    verify(readListener).onError(ioException);
  }

  @Test
  void shouldInvoke_OnError_IfAnErrorOccursInMethod_OnAllDataRead() throws IOException {
    readChars(10);
    IOException ioException = new IOException();
    doThrow(ioException).when(readListener).onAllDataRead();

    inputStream.setReadListener(readListener);

    verify(readListener).onError(ioException);
  }

  @Test
  void shouldInvoke_OnError_AndThrowAnExceptionIfAnErrorOccursWhileIsReadingLastByte()
      throws IOException {
    readChars(9);
    IOException ioException = new IOException();
    doThrow(ioException).when(readListener).onAllDataRead();
    inputStream.setReadListener(readListener);

    assertThrows(IOException.class, () -> inputStream.read());
    verify(readListener).onError(ioException);
  }

  private void readChars(int times) throws IOException {
    for (int i = 0; i < times; i++) {
      inputStream.read();
    }
  }
}