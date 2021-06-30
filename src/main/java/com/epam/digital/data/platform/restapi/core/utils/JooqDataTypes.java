/*
 * Copyright 2021 EPAM Systems.
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

package com.epam.digital.data.platform.restapi.core.utils;

import com.epam.digital.data.platform.model.core.kafka.File;
import com.epam.digital.data.platform.restapi.core.converter.jooq.FileConverter;
import com.epam.digital.data.platform.restapi.core.converter.jooq.FileListConverter;
import java.util.List;
import org.jooq.DataType;
import org.jooq.impl.DefaultDataType;
import org.jooq.impl.SQLDataType;

public final class JooqDataTypes {

  public static final DataType<File> FILE_DATA_TYPE =
      SQLDataType.OTHER.asConvertedDataType(new FileConverter());

  public static final DataType<List> FILE_ARRAY_DATA_TYPE =
      SQLDataType.OTHER.asConvertedDataType(new FileListConverter());

  public static final DataType<Object[]> ARRAY_DATA_TYPE =
      DefaultDataType.getDefaultDataType("java.util.Collection").getArrayDataType();

  private JooqDataTypes() {}
}
