package com.epam.digital.data.platform.restapi.core.converter.jooq;

import static org.assertj.core.api.Assertions.assertThat;

import com.epam.digital.data.platform.model.core.kafka.File;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

class FileConverterTest {

  private static final String FILE_DB_TYPE = "type_file";
  private static final String FILE_ID = "id";
  private static final String FILE_CHECKSUM = "checksum";

  private final FileConverter fileConverter = new FileConverter();

  @Test
  void expectValidConvertionFromPgObjectToFileObject() throws SQLException {
    var pgObject = new PGobject();
    pgObject.setType(FILE_DB_TYPE);
    pgObject.setValue(String.format("(%s,%s)", FILE_ID, FILE_CHECKSUM));

    File actual = fileConverter.from(pgObject);

    assertThat(actual.getId()).isEqualTo(FILE_ID);
    assertThat(actual.getChecksum()).isEqualTo(FILE_CHECKSUM);
  }

  @Test
  void expectValidConvertionFromRandomObjectToFileObject() {
    var o = new Object();

    File actual = fileConverter.from(o);

    assertThat(actual).isNull();
  }

  @Test
  void expectValidConvertionFromFileToObject() {
    var file = new File();
    file.setId(FILE_ID);
    file.setChecksum(FILE_CHECKSUM);

    Object actual = fileConverter.to(file);

    var actualPgObject = (PGobject) actual;

    assertThat(actualPgObject.getType()).isEqualTo(FILE_DB_TYPE);
    assertThat(actualPgObject.getValue()).isEqualTo(String.format("(%s,%s)", FILE_ID, FILE_CHECKSUM));
  }

  @Test
  void returnNullWhenNullGiven() {
    assertThat(fileConverter.from(null)).isNull();
  }

  @Test
  void expectEmptyPgObjectWhenNullGiven() {
    var res = (PGobject) fileConverter.to(null);

    assertThat(res.getValue()).isNull();
  }
}
