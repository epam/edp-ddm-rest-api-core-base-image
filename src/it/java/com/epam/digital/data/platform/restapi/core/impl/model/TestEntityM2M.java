package com.epam.digital.data.platform.restapi.core.impl.model;

import java.util.List;
import java.util.UUID;

public class TestEntityM2M {

  private UUID id;
  private String name;
  private List<UUID> entities;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public List<UUID> getEntities() {
    return entities;
  }

  public void setEntities(List<UUID> entities) {
    this.entities = entities;
  }
}
