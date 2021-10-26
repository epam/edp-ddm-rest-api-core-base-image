package com.epam.digital.data.platform.restapi.core.util;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.RequestContext;
import com.epam.digital.data.platform.model.core.kafka.SecurityContext;

public class SearchHandlerTestUtil {
  public static  <I> Request<I> mockRequest(I criteria) {
    return new Request<>(criteria, new RequestContext(), new SecurityContext());
  }
}
