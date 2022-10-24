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

package com.epam.digital.data.platform.restapi.core.config;

import com.fasterxml.jackson.core.FormatSchema;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Function;

@Configuration
public class CsvConfig {

  @Bean
  public CsvMapper csvMapper() {
    var csvMapper = new CsvMapper();
    csvMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    csvMapper.registerModule(new JavaTimeModule());
    csvMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    csvMapper.enable(CsvParser.Feature.TRIM_SPACES);
    csvMapper.enable(CsvParser.Feature.FAIL_ON_MISSING_COLUMNS);
    return csvMapper;
  }

  @Bean
  public FormatSchema csvFormatSchema() {
    return CsvSchema.emptySchema()
            .withHeader()
            .withColumnSeparator(';')
            .withArrayElementSeparator(",");
  }

  @Bean
  public Function<Class<?>, ObjectReader> csvReaderFactory(CsvMapper csvMapper, FormatSchema csvFormatSchema) {
    return cl -> csvMapper
            .readerFor(cl)
            .with(csvFormatSchema);
  }
}
