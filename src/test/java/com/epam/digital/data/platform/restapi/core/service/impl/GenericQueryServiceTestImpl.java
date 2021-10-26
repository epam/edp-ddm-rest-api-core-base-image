package com.epam.digital.data.platform.restapi.core.service.impl;

import com.epam.digital.data.platform.restapi.core.dto.MockEntity;
import com.epam.digital.data.platform.restapi.core.queryhandler.AbstractQueryHandler;
import com.epam.digital.data.platform.restapi.core.service.GenericQueryService;
import java.util.UUID;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class GenericQueryServiceTestImpl extends GenericQueryService<UUID, MockEntity> {

  protected GenericQueryServiceTestImpl(
      AbstractQueryHandler<UUID, MockEntity> queryHandler) {
    super(queryHandler);
  }
}
