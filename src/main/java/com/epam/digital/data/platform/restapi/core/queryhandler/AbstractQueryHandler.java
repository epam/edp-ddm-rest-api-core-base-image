package com.epam.digital.data.platform.restapi.core.queryhandler;

import com.epam.digital.data.platform.model.core.kafka.Request;
import com.epam.digital.data.platform.restapi.core.annotation.DatabaseOperation;
import com.epam.digital.data.platform.restapi.core.annotation.DatabaseOperation.Operation;
import com.epam.digital.data.platform.restapi.core.exception.ForbiddenOperationException;
import com.epam.digital.data.platform.restapi.core.exception.SqlErrorException;
import com.epam.digital.data.platform.restapi.core.service.AccessPermissionService;
import com.epam.digital.data.platform.restapi.core.service.JwtInfoProvider;
import com.epam.digital.data.platform.starter.security.dto.JwtClaimsDto;
import java.util.List;
import java.util.Optional;
import org.jooq.DSLContext;
import org.jooq.SelectFieldOrAsterisk;
import org.jooq.impl.DSL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractQueryHandler<I, O> implements
    com.epam.digital.data.platform.restapi.core.queryhandler.QueryHandler<I, O> {

  private final Logger log = LoggerFactory.getLogger(AbstractQueryHandler.class);

  @Autowired
  protected DSLContext context;
  @Autowired
  protected JwtInfoProvider jwtInfoProvider;

  protected final AccessPermissionService<O> accessPermissionService;

  protected AbstractQueryHandler(AccessPermissionService<O> accessPermissionService) {
    this.accessPermissionService = accessPermissionService;
  }

  @DatabaseOperation(Operation.READ)
  @Override
  public Optional<O> findById(Request<I> input) {
    log.info("Reading from DB");

    JwtClaimsDto userClaims = jwtInfoProvider.getUserClaims(input);
    if (!accessPermissionService.hasReadAccess(tableName(), userClaims, entityType())) {
      throw new ForbiddenOperationException("User has invalid role for search by ID");
    }

    I id = input.getPayload();
    try {
      final O dto =
          context
              .select(selectFields())
              .from(DSL.table(tableName()))
              .where(DSL.field(idName()).eq(id))
              .fetchOneInto(entityType());
      return Optional.ofNullable(dto);
    } catch (Exception e) {
      throw new SqlErrorException("Can not read from DB", e);
    }
  }

  public abstract String idName();

  public abstract String tableName();

  public abstract Class<O> entityType();

  public abstract List<SelectFieldOrAsterisk> selectFields();
}
