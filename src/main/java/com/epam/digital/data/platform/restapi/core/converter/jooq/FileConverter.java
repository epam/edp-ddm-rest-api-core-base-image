package com.epam.digital.data.platform.restapi.core.converter.jooq;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.exception.RequestProcessingException;
import java.sql.SQLException;
import org.jooq.Converter;
import org.postgresql.util.PGobject;

public class FileConverter implements Converter<Object, File> {

  private static final String DB_TYPE = "type_file";

  @Override
  public File from(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof PGobject) {
      var pgObject = (PGobject) o;
      var file = new File();
      var valueStr = pgObject.getValue();
      String[] arr = valueStr.substring(1, valueStr.length() - 1).split(",");
      file.setId(arr[0]);
      file.setChecksum(arr[1]);
      return file;
    } else {
      return null;
    }
  }

  @Override
  public Object to(File file) {
    var pgObject = new PGobject();
    pgObject.setType(DB_TYPE);
    try {
      pgObject.setValue(convertFileToPgObject(file));
    } catch (SQLException e) {
      throw new RequestProcessingException("Exception", Status.OPERATION_FAILED);
    }
    return pgObject;
  }

  @Override
  public Class<File> toType() {
    return File.class;
  }

  @Override
  public Class<Object> fromType() {
    return Object.class;
  }

  private String convertFileToPgObject(File file) {
    if (file == null) {
      return null;
    }
    return "(" + file.getId() + "," + file.getChecksum() + ")";
  }
}
