package com.epam.digital.data.platform.restapi.core.impl.model;

import java.util.List;
import java.util.UUID;

public class TestEntityM2M {

  private UUID id;
  private String name;
  private TestEntity[] entities;

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

  public TestEntity[] getEntities() {
    return entities;
  }

  public void setEntities(TestEntity[] entities) {
    this.entities = entities;
  }
}
