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
