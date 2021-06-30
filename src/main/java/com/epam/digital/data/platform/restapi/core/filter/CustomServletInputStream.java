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

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CustomServletInputStream extends ServletInputStream {

  private byte[] bytes;

  private int lastIndexRetrieved = -1;
  private ReadListener readListener = null;

  public CustomServletInputStream(String s) {
    bytes = s.getBytes(StandardCharsets.UTF_8);
  }

  public CustomServletInputStream(byte[] inputBytes) {
    bytes = inputBytes;
  }

  @Override
  public boolean isFinished() {
    return (lastIndexRetrieved == bytes.length - 1);
  }

  @Override
  public boolean isReady() {
    return isFinished();
  }

  @Override
  public void setReadListener(ReadListener readListener) {
    this.readListener = readListener;
    if (!isFinished()) {
      try {
        readListener.onDataAvailable();
      } catch (IOException e) {
        readListener.onError(e);
      }
    } else {
      try {
        readListener.onAllDataRead();
      } catch (IOException e) {
        readListener.onError(e);
      }
    }
  }

  @Override
  public int read() throws IOException {
    int i;
    if (!isFinished()) {
      i = bytes[lastIndexRetrieved + 1];
      lastIndexRetrieved++;
      if (isFinished() && (readListener != null)) {
        try {
          readListener.onAllDataRead();
        } catch (IOException ex) {
          readListener.onError(ex);
          throw ex;
        }
      }
      return i;
    } else {
      return -1;
    }
  }
}
