/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.digital.data.platform.restapi.core.util;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntity;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityFile;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityFileArray;
import com.epam.digital.data.platform.restapi.core.impl.model.TestEntityM2M;
import com.epam.digital.data.platform.restapi.core.impl.model.TypGender;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class DaoTestUtils {

  public static final UUID TEST_ENTITY_ID = UUID
      .fromString("3cc262c1-0cd8-4d45-be66-eb0fca821e0a");
  public static final UUID TEST_ENTITY_ID_2 = UUID
      .fromString("9ce4cad9-ff50-4fa3-b893-e07afea0cb8d");
  public static final LocalDateTime PD_PROCESSING_CONSENT_CONSENT_DATE = LocalDateTime
      .of(2020, 1, 15, 12, 0, 1);
  public static final UUID TEST_ENTITY_FILE_ID = UUID
      .fromString("7f017d37-6ba5-4849-a4b2-f6a3ef2cadb9");
  public static final UUID TEST_ENTITY_FILE_ARRAY_ID = UUID
      .fromString("7f017d37-6ba5-4849-a4b2-f6a3ef2cadb9");
  public static final UUID TEST_ENTITY_M2M_ID = UUID
      .fromString("7f017d37-6ba5-4849-a4b2-f6a3ef2cadb9");

  public static final TestEntity TEST_ENTITY = testEntity();
  public static final TestEntity TEST_ENTITY_2 = testEntity2();
  public static final TestEntityFile TEST_ENTITY_FILE = testEntityFile();
  public static final TestEntityFileArray TEST_ENTITY_FILE_ARRAY = testEntityFileArray();
  public static final TestEntityM2M TEST_ENTITY_M2M = testEntityM2M();

  private DaoTestUtils() {
  }

  public static TestEntity testEntity() {
    var r = new TestEntity();
    r.setId(TEST_ENTITY_ID);
    r.setPersonFullName("John Doe Patronymic");
    r.setPersonPassNumber("AB123456");
    r.setConsentDate(PD_PROCESSING_CONSENT_CONSENT_DATE);
    r.setPersonGender(TypGender.M);
    return r;
  }

  public static TestEntity testEntity2() {
    var r = new TestEntity();
    r.setId(TEST_ENTITY_ID_2);
    r.setPersonFullName("Benjamin Franklin Patronymic");
    r.setPersonPassNumber("XY098765");
    r.setConsentDate(PD_PROCESSING_CONSENT_CONSENT_DATE);
    r.setPersonGender(TypGender.M);
    return r;
  }

  public static TestEntityFile testEntityFile() {
    var r = new TestEntityFile();
    r.setId(TEST_ENTITY_FILE_ID);
    r.setLegalEntityName("FOP John Doe");
    r.setScanCopy(new File("1", "0d5f97dd25b50267a1c03fba4d649d56d3d818704d0dcdfa692db62119b1221a"));
    return r;
  }

  public static TestEntityFileArray testEntityFileArray() {
    var r = new TestEntityFileArray();
    r.setId(TEST_ENTITY_FILE_ARRAY_ID);
    r.setLegalEntityName("FOP John Doe");
    r.setScanCopies(List.of(
        new File("1", "db7bb0ef3ae21cafba57068bab4bcdd5129ba8a25ef5f8c16ad33fc686c7467e"),
        new File("2", "2a3d2db6e3974ee0fae45b0a3f0616c645b8a80b72c153d0577b35cbdfe41dd4")
    ));
    return r;
  }

  public static TestEntityM2M testEntityM2M() {
    var entityM2M = new TestEntityM2M();
    entityM2M.setId(TEST_ENTITY_M2M_ID);
    entityM2M.setName("FOP John Doe");
    entityM2M.setEntities(new TestEntity[] { testEntity(), testEntity2() });
    return entityM2M;
  }
}
