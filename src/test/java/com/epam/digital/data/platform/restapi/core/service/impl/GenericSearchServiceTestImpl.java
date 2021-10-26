package com.epam.digital.data.platform.restapi.core.service.impl;

import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.searchhandler.AbstractSearchHandler;
import com.epam.digital.data.platform.restapi.core.service.GenericSearchService;
import com.epam.digital.data.platform.restapi.core.util.MockEntityContains;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class GenericSearchServiceTestImpl
    extends GenericSearchService<MockEntityContains, MockEntity> {

  protected GenericSearchServiceTestImpl(
      AbstractSearchHandler<MockEntityContains, MockEntity> searchHandler) {
    super(searchHandler);
  }
}
