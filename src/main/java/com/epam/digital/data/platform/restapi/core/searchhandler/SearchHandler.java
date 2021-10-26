package com.epam.digital.data.platform.restapi.core.searchhandler;

import com.epam.digital.data.platform.model.core.kafka.Request;
import org.springframework.cloud.sleuth.annotation.NewSpan;

import java.util.List;

public interface SearchHandler<I, O> {

  @NewSpan
  List<O> search(Request<I> searchCriteria);
}
