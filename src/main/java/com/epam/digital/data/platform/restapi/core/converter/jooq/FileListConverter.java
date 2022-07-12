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

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.model.core.kafka.Status;
import com.epam.digital.data.platform.restapi.core.exception.RequestProcessingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.jooq.Converter;
import org.postgresql.jdbc.PgArray;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileListConverter implements Converter<Object, List> {

  private final Logger log = LoggerFactory.getLogger(FileListConverter.class);
  
  private static final String DB_TYPE = "_type_file";

  @Override
  public List from(Object o) {
    if (o == null) {
      return null;
    }
    if (o instanceof PgArray) {
      Object obj;
      try {
        obj = ((PgArray) o).getArray();
      } catch (SQLException e) {
        log.error("Cannot cast Object to PgArray", e);
        throw new RequestProcessingException("Exception", Status.OPERATION_FAILED);
      }
      
      Object[] array;
      if(obj instanceof Object[]) {
        array = (Object[]) obj; 
      } else {
        return null;
      }
      
      var files = new ArrayList<File>();
      for (Object tempObject : array) {
        if(tempObject instanceof PGobject) {
          var pgObject = (PGobject) tempObject;
          var file = new File();
          var valueStr = pgObject.getValue();
          String[] arr = valueStr.substring(1, valueStr.length() - 1).split(",");
          file.setId(arr[0]);
          file.setChecksum(arr[1]);
          files.add(file);
        }
      }
      return files;
    } else {
      return null;
    }
  }

  @Override
  public Object to(List files) {
    var pgObject = new PGobject();
    pgObject.setType(DB_TYPE);
    try {
      pgObject.setValue(convertFileListToPgObject(files));
    } catch (SQLException e) {
      log.error("Cannot set value to PgObject with type '_type_file'" , e);
      throw new RequestProcessingException("Exception", Status.OPERATION_FAILED);
    } catch (ClassCastException e) {
      log.error("Cannot cast List<Object> to List<File>" , e);
      throw new RequestProcessingException("Exception", Status.OPERATION_FAILED);
    }
    return pgObject;
  }

  @Override
  public Class<List> toType() {
    return List.class;
  }

  @Override
  public Class<Object> fromType() {
    return Object.class;
  }

  private String convertFileListToPgObject(List<File> files) {
    if (files == null || files.isEmpty()) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    for(var file : files) {
      sb.append("\"(").append(file.getId()).append(",").append(file.getChecksum()).append(")\",");
    }
    sb.deleteCharAt(sb.length() - 1);
    sb.append("}");
    return sb.toString();
  }
}
