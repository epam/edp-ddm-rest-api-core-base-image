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

package com.epam.digital.data.platform.restapi.core.config;

import com.epam.digital.data.platform.restapi.core.config.properties.KafkaProperties;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.SeekToCurrentErrorHandler;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

  private static final String CERTIFICATES_TYPE = "PEM";
  private static final String SECURITY_PROTOCOL = "SSL";
  public static final String SSL_TRUSTSTORE_CERTIFICATES = "ssl.truststore.certificates";
  public static final String SSL_KEYSTORE_CERTIFICATE_CHAIN = "ssl.keystore.certificate.chain";
  public static final String SSL_KEYSTORE_KEY = "ssl.keystore.key";

  @Autowired
  KafkaProperties kafkaProperties;

  @Bean
  public Map<String, Object> consumerConfigs() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrap());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    props.put(
        JsonDeserializer.TRUSTED_PACKAGES,
        String.join(",", kafkaProperties.getTrustedPackages()));
    props.put(ConsumerConfig.GROUP_ID_CONFIG, kafkaProperties.getGroupId());
    if (kafkaProperties.getSsl().isEnabled()) {
      props.putAll(createSslProperties());
    }
    return props;
  }

  @Bean
  public Map<String, Object> producerConfigs() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrap());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    if (kafkaProperties.getSsl().isEnabled()) {
      props.putAll(createSslProperties());
    }
    return props;
  }

  private Map<String, Object> createSslProperties() {
    return Map.of(
        CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SECURITY_PROTOCOL,
        SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, CERTIFICATES_TYPE,
        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, CERTIFICATES_TYPE,
        SSL_TRUSTSTORE_CERTIFICATES, kafkaProperties.getSsl().getTruststoreCertificate(),
        SSL_KEYSTORE_CERTIFICATE_CHAIN, kafkaProperties.getSsl().getKeystoreCertificate(),
        SSL_KEYSTORE_KEY, kafkaProperties.getSsl().getKeystoreKey()
    );
  }

  @Bean
  @Primary
  public <I> ProducerFactory<String, I> requestProducerFactory() {
    return new DefaultKafkaProducerFactory<>(producerConfigs());
  }

  @Bean
  public <O> ConsumerFactory<String, O> replyConsumerFactory() {
    return new DefaultKafkaConsumerFactory<>(consumerConfigs());
  }

  @Bean
  public <I, O> ReplyingKafkaTemplate<String, I, O> replyingKafkaTemplate(
      ProducerFactory<String, I> pf, ConcurrentKafkaListenerContainerFactory<String, O> factory) {
    String[] outboundTopics = kafkaProperties.getTopics().values().stream()
            .map(KafkaProperties.Handler::getReplay)
            .toArray(String[]::new);
    ConcurrentMessageListenerContainer<String, O> replyContainer = factory.createContainer(outboundTopics);
    replyContainer.getContainerProperties().setMissingTopicsFatal(false);
    replyContainer.getContainerProperties().setGroupId(UUID.randomUUID().toString());
    ReplyingKafkaTemplate<String, I, O> kafkaTemplate = new ReplyingKafkaTemplate<>(pf, replyContainer);
    kafkaTemplate.setSharedReplyTopic(true);
    kafkaTemplate.setDefaultReplyTimeout(Duration.ofSeconds(30L));
    return kafkaTemplate;
  }

  @Bean
  public <O>
  ConcurrentKafkaListenerContainerFactory<String, O> concurrentKafkaListenerContainerFactory(
      ConsumerFactory<String, O> cf) {
    ConcurrentKafkaListenerContainerFactory<String, O> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(cf);
    return factory;
  }

  @Bean
  public <O> KafkaTemplate<String, O> replyTemplate(
      ProducerFactory<String, O> pf, ConcurrentKafkaListenerContainerFactory<String, O> factory) {
    KafkaTemplate<String, O> kafkaTemplate = new KafkaTemplate<>(pf);
    factory.getContainerProperties().setMissingTopicsFatal(false);
    factory.setReplyTemplate(kafkaTemplate);
    factory.setErrorHandler(deadLetterErrorHandler(kafkaTemplate));
    return kafkaTemplate;
  }

  @Bean
  public AdminClient kafkaAdminClient() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaProperties.getBootstrap());
    if (kafkaProperties.getSsl().isEnabled()) {
      props.putAll(createSslProperties());
    }
    return KafkaAdminClient.create(props);
  }

  private SeekToCurrentErrorHandler deadLetterErrorHandler(
      KafkaOperations<String, ?> kafkaTemplate) {
    DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
    ExponentialBackOff backOff = new ExponentialBackOff(
        kafkaProperties.getErrorHandler().getInitialInterval(),
        kafkaProperties.getErrorHandler().getMultiplier());
    backOff.setMaxElapsedTime(kafkaProperties.getErrorHandler().getMaxElapsedTime());
    return new SeekToCurrentErrorHandler(recoverer, backOff);
  }
}
