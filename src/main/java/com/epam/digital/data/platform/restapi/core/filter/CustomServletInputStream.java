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
