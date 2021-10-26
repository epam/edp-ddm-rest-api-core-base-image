package com.epam.digital.data.platform.restapi.core.service;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.model.core.kafka.Response;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.exception.RequestProcessingException;
import com.epam.digital.data.platform.restapi.core.searchhandler.AbstractSearchHandler;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class GenericSearchService<I, O> {

  private static final String INPUT_IS_INVALID = "Input is invalid";

  private final Logger log = LoggerFactory.getLogger(GenericSearchService.class);

  @Autowired
  private JwtValidationService jwtValidationService;

  private final AbstractSearchHandler<I, O> searchHandler;

  protected GenericSearchService(AbstractSearchHandler<I, O> searchHandler) {
    this.searchHandler = searchHandler;
  }

  public Response<List<O>> request(Request<I> input) {
    Response<List<O>> response = new Response<>();

    try {
      if (!isInputValid(input, response)) {
        log.info(INPUT_IS_INVALID);
        return response;
      }

      List<O> found = searchHandler.search(input);
      response.setPayload(found);
      response.setStatus(Status.SUCCESS);
    } catch (RequestProcessingException e) {
      log.error("Exception while request processing", e);
      response.setStatus(e.getKafkaResponseStatus());
      response.setDetails(e.getDetails());
    } catch (Exception e) {
      log.error("Unexpected exception while executing the 'delete' method", e);
      response.setStatus(Status.OPERATION_FAILED);
      response.setDetails("Unexpected exception while executing the 'delete' method");
    }

    return response;
  }

  private <T, U> boolean isInputValid(Request<T> input, Response<U> response) {
    if (!jwtValidationService.isValid(input)) {
      response.setStatus(Status.JWT_INVALID);
      return false;
    }

    return true;
  }
}
