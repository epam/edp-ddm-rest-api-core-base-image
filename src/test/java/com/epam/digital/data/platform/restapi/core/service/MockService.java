package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import org.springframework.boot.test.context.TestComponent;

import java.util.UUID;

import static org.mockito.Mockito.mock;

@TestComponent
public class MockService {

  public Response<MockEntity> search(Object searchConditions) {
    return mock(Response.class);
  }

  public Response<MockEntity> read(Request<UUID> request) {
    return mock(Response.class);
  }

  public Response<Void> create(Request<MockEntity> request) {
    return mock(Response.class);
  }

  public Response<Void> update(Request<MockEntity> request) {
    return mock(Response.class);
  }

  public Response<Void> delete(Request<MockEntity> request) {
    return mock(Response.class);
  }
}
