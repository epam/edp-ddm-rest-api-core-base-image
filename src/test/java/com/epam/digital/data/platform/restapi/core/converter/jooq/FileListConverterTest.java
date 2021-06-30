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

package com.epam.digital.data.platform.restapi.core.converter.jooq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.restapi.core.exception.RequestProcessingException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;

@ExtendWith(MockitoExtension.class)
class FileListConverterTest {

  private static final String FILE_DB_TYPE = "type_file";
  private static final String FILE_ARRAY_DB_TYPE = "_type_file";

  private final FileListConverter fileListConverter = new FileListConverter();

  @Mock
  PgArray pgArray;

  @Nested
  class From {

    @Test
    void expectValidConversionFromPgObjectToFileObject() throws SQLException {
      when(pgArray.getArray()).thenReturn(mockArray());

      List<File> actual = fileListConverter.from(pgArray);

      assertThat(actual).hasSize(3);
      assertThat(actual.get(0).getId()).isEqualTo("id1");
      assertThat(actual.get(0).getChecksum()).isEqualTo("sum1");

      assertThat(actual.get(1).getId()).isEqualTo("id2");
      assertThat(actual.get(1).getChecksum()).isEqualTo("sum2");

      assertThat(actual.get(2).getId()).isEqualTo("id3");
      assertThat(actual.get(2).getChecksum()).isEqualTo("sum3");
    }

    @Test
    void notPgArrayConvertsToNull() {
      var o = new Object();

      var actual = fileListConverter.from(o);

      assertThat(actual).isNull();
    }

    @Test
    void notArrayOfObjectsConvertsToNull() throws SQLException {
      when(pgArray.getArray()).thenReturn(new Object());

      var actual = fileListConverter.from(pgArray);

      assertThat(actual).isNull();
    }

    @Test
    void arrayOfNotPgObjectsConvertsToEmptyList() throws SQLException {
      when(pgArray.getArray()).thenReturn(new Object[]{new Object(), new Object()});

      var actual = fileListConverter.from(pgArray);

      assertThat(actual).isEmpty();
    }


    @Test
    void emptyArrayConvertsToEmptyList() throws SQLException {
      when(pgArray.getArray()).thenReturn(new Object[]{});

      var actual = fileListConverter.from(pgArray);

      assertThat(actual).isEmpty();
    }

    @Test
    void returnNullWhenNullGiven() {
      assertNull(fileListConverter.from(null));
    }

    @Test
    void shouldConvertSQLExceptionToRequestProcessingException() throws SQLException {
      when(pgArray.getArray()).thenThrow(SQLException.class);

      assertThrows(RequestProcessingException.class, () -> fileListConverter.from(pgArray));
    }
  }

  @Nested
  class To {

    @Test
    void expectEmptyPgObjectWhenNullGiven() {
      var res = (PGobject) fileListConverter.to(null);

      assertThat(res.getValue()).isNull();
    }

    @Test
    void expectEmptyPgObjectWhenEmptyListGiven() {
      var res = (PGobject) fileListConverter.to(Collections.emptyList());

      assertThat(res.getValue()).isNull();
    }

    @Test
    void expectClassCastExceptionConvertedToRequestProcessingExceptionWhenGivenListObject() {
      assertThrows(RequestProcessingException.class,
          () -> fileListConverter.to(List.of(new Object())));
    }

    @Test
    void shouldReturnValidPgObject() {
      var files = List.of(
          new File("id1", "sum1"),
          new File("id2", "sum2"),
          new File("id3", "sum3")
      );
      var result = (PGobject) fileListConverter.to(files);

      assertThat(result.getType()).isEqualTo(FILE_ARRAY_DB_TYPE);
      assertThat(result.getValue()).isEqualTo("{\"(id1,sum1)\",\"(id2,sum2)\",\"(id3,sum3)\"}");
    }
  }

  private Object[] mockArray() throws SQLException {
    var pgObject1 = filePgObject("id1", "sum1");
    var pgObject2 = filePgObject("id2", "sum2");
    var pgObject3 = filePgObject("id3", "sum3");

    return new Object[]{pgObject1, pgObject2, pgObject3};
  }

  private PGobject filePgObject(String id, String checksum) throws SQLException {
    var pgObject = new PGobject();
    pgObject.setType(FILE_DB_TYPE);
    pgObject.setValue(String.format("(%s,%s)", id, checksum));
    return pgObject;
  }
}
