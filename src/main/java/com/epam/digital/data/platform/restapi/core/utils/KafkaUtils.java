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

import java.util.Objects;
import java.util.Optional;
import java.util.stream.StreamSupport;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

public final class KafkaUtils {

  private KafkaUtils() {}

  public static <T> Optional<String> getKafkaHeader(
      ConsumerRecord<String, T> responseRecord, String searchedHeader) {
    return StreamSupport.stream(responseRecord.headers().spliterator(), false)
        .filter(header -> Objects.equals(searchedHeader, header.key()))
        .map(Header::value)
        .map(String::new)
        .findFirst();
  }
}
