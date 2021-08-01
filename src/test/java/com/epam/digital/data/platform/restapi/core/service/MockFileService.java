package com.epam.digital.data.platform.restapi.core.service;

import static org.mockito.Mockito.mock;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.restapi.core.dto.MockEntityFile;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class MockFileService {

  public Response<MockEntityFile> search(Object searchConditions) {
    return mock(Response.class);
  }

  public Response<MockEntityFile> read(Request<UUID> request) {
    return mock(Response.class);
  }

  public Response<Void> create(Request<MockEntityFile> request) {
    return mock(Response.class);
  }

  public Response<Void> update(Request<MockEntityFile> request) {
    return mock(Response.class);
  }

  public Response<Void> delete(Request<MockEntityFile> request) {
    return mock(Response.class);
  }
}
