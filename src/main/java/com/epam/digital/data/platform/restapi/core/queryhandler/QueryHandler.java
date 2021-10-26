package com.epam.digital.data.platform.restapi.core.queryhandler;

import com.epam.digital.data.platform.model.core.kafka.Request;
import org.springframework.cloud.sleuth.annotation.NewSpan;

import java.util.Optional;

public interface QueryHandler<I, O> {

  @NewSpan
  Optional<O> findById(Request<I> input);
}
