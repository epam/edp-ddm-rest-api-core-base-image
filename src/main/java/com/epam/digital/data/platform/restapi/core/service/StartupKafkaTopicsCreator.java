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

package com.epam.digital.data.platform.restapi.core.service;

import static org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG;

import com.epam.digital.data.platform.restapi.core.config.properties.KafkaProperties;
import com.epam.digital.data.platform.restapi.core.exception.CreateKafkaTopicException;
import com.epam.digital.data.platform.restapi.core.config.properties.KafkaProperties.Handler;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.stereotype.Component;

@Component
public class StartupKafkaTopicsCreator {

  private static final long DAYS_TO_MS = 24 * 60 * 60 * 1000L;
  private static final Long TOPIC_CREATION_TIMEOUT = 60L;

  private static final String READ = "read";
  private static final String SEARCH = "search";

  private final AdminClient kafkaAdminClient;
  private final KafkaProperties kafkaProperties;

  public StartupKafkaTopicsCreator(AdminClient kafkaAdminClient,
      KafkaProperties kafkaProperties) {
    this.kafkaAdminClient = kafkaAdminClient;
    this.kafkaProperties = kafkaProperties;
  }

  @PostConstruct
  public void createKafkaTopics() {

    Set<String> missingTopicNames = getMissingTopicNames();

    var createTopicsResult = kafkaAdminClient.createTopics(getNewTopics(missingTopicNames));
    try {
      createTopicsResult.all().get(TOPIC_CREATION_TIMEOUT, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new CreateKafkaTopicException(
          String.format("Failed to create kafka topics %s in %d sec", missingTopicNames,
              TOPIC_CREATION_TIMEOUT), e);
    }
  }

  private Set<String> getMissingTopicNames() {
    Set<String> existingTopics;
    try {
      existingTopics = kafkaAdminClient.listTopics().names()
          .get(TOPIC_CREATION_TIMEOUT, TimeUnit.SECONDS);
    } catch (Exception e) {
      throw new CreateKafkaTopicException(String.format(
          "Failed to retrieve existing kafka topics in %d sec", TOPIC_CREATION_TIMEOUT), e);
    }
    Set<String> requiredTopics = getRequiredTopics(kafkaProperties.getTopics());
    requiredTopics.removeAll(existingTopics);
    return requiredTopics;
  }

  private Set<String> getRequiredTopics(Map<String, Handler> rootsOfTopicNames) {
    Set<String> requiredTopics = new HashSet<>();
    for (Entry<String, Handler> e : rootsOfTopicNames.entrySet()) {
      requiredTopics.add(e.getValue().getRequest());
      requiredTopics.add(e.getValue().getRequest() + ".DLT");
      requiredTopics.add(e.getValue().getReplay());
    }
    return requiredTopics;
  }

  private Collection<NewTopic> getNewTopics(Set<String> requiredTopics) {
    return requiredTopics.stream()
        .map(topicName -> new NewTopic(topicName,
            kafkaProperties.getTopicProperties().getNumPartitions(),
            kafkaProperties.getTopicProperties().getReplicationFactor()))
        .map(topic -> topic.configs(getRetentionPolicy(topic.name())))
        .collect(Collectors.toSet());
  }

  private Map<String, String> getRetentionPolicy(String topicName) {

    var retentionPolicyInDays = kafkaProperties.getTopicProperties().getRetentionPolicyInDays();

    int days = retentionPolicyInDays.getWrite();
    if (topicName.startsWith(READ) || topicName.startsWith(SEARCH)) {
      days = retentionPolicyInDays.getRead();
    }

    return Map.of(RETENTION_MS_CONFIG, Long.toString(days * DAYS_TO_MS));
  }
}
