package com.epam.digital.data.platform.restapi.core.service;

import static org.apache.kafka.common.config.TopicConfig.RETENTION_MS_CONFIG;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.restapi.core.config.properties.KafkaProperties;
import com.epam.digital.data.platform.restapi.core.config.properties.KafkaProperties.ErrorHandler;
import com.epam.digital.data.platform.restapi.core.config.properties.KafkaProperties.Handler;
import com.epam.digital.data.platform.restapi.core.config.properties.KafkaProperties.RetentionPolicyInDays;
import com.epam.digital.data.platform.restapi.core.config.properties.KafkaProperties.TopicProperties;
import com.epam.digital.data.platform.restapi.core.exception.CreateKafkaTopicException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.KafkaFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StartupKafkaTopicsCreatorTest {

  private static final String topicsRootName = "test";
  private static final Set<String> existedTopics = Set.of(topicsRootName + "-inbound", "some-topic");

  private static final long DAYS_TO_MS = 24 * 60 * 60 * 1000L;

  private static final int RETENTION_DAYS_FOR_READ = 2;
  private static final String RETENTION_MS_FOR_READ = Long.toString(RETENTION_DAYS_FOR_READ * DAYS_TO_MS);

  private static final int RETENTION_DAYS_FOR_WRITE = 365;
  private static final String RETENTION_MS_FOR_WRITE = Long.toString(RETENTION_DAYS_FOR_WRITE * DAYS_TO_MS);

  private KafkaProperties kafkaProperties;
  private StartupKafkaTopicsCreator startupKafkaTopicsCreator;

  @Mock
  private AdminClient adminClient;
  @Mock
  private KafkaFuture<Void> createTopicsFuture;
  @Mock
  private KafkaFuture<Set<String>> listTopicsFuture;
  @Mock
  private CreateTopicsResult createTopicsResult;
  @Mock
  private ListTopicsResult listTopicsResult;
  @Captor
  private ArgumentCaptor<Set<NewTopic>> argumentCaptor;

  @BeforeEach
  void setup() {
    kafkaProperties = createKafkaProperties(topicsRootName);
    startupKafkaTopicsCreator = new StartupKafkaTopicsCreator(adminClient, kafkaProperties);
  }

  @Test
  void shouldCreateAllMissingTopics() throws Exception {
    customizeAdminClientMock(existedTopics);

    startupKafkaTopicsCreator.createKafkaTopics();

    verify(adminClient).createTopics(argumentCaptor.capture());

    Set<String> topicsForCreation = getTopicsForCreation(argumentCaptor);
    Set<String> missingTopics = getMissingTopics(topicsRootName);

    assertEquals(missingTopics, topicsForCreation);
  }

  @Test
  void shouldThrowExceptionWhenCannotConnectToKafka() {
    when(adminClient.listTopics()).thenThrow(new CreateKafkaTopicException("any", null));

    assertThrows(CreateKafkaTopicException.class,
        () -> startupKafkaTopicsCreator.createKafkaTopics());
  }

  @ParameterizedTest
  @MethodSource("provideParameters")
  void shouldSetCorrectRetentionPolicy(String topicsRootName, String retentionMs) throws Exception {

    // given
    kafkaProperties = createKafkaProperties(topicsRootName);
    startupKafkaTopicsCreator = new StartupKafkaTopicsCreator(adminClient, kafkaProperties);
    customizeAdminClientMock(existedTopics);

    // when
    startupKafkaTopicsCreator.createKafkaTopics();

    // then
    verify(adminClient).createTopics(argumentCaptor.capture());
    Set<NewTopic> createdTopics = argumentCaptor.getValue();

    var topicsWithCorrectRetentionTime = argumentCaptor.getValue().stream()
        .filter(x -> x.configs().get(RETENTION_MS_CONFIG).equals(retentionMs))
        .collect(Collectors.toSet());

    assertEquals(createdTopics.size(), topicsWithCorrectRetentionTime.size());
  }

  private static Stream<Arguments> provideParameters() {
    return Stream.of(
        Arguments.of("read", RETENTION_MS_FOR_READ),
        Arguments.of("search", RETENTION_MS_FOR_READ),
        Arguments.of("any_other", RETENTION_MS_FOR_WRITE)
    );
  }

  private Set<String> getTopicsForCreation(ArgumentCaptor<Set<NewTopic>> setArgumentCaptor) {
    return setArgumentCaptor.getValue()
        .stream()
        .map(NewTopic::name)
        .collect(Collectors.toSet());
  }

  private Set<String> getMissingTopics(String topicsRootName) {
    Set<String> requiredTopics = new HashSet<>();
    requiredTopics.add(topicsRootName + "-inbound");
    requiredTopics.add(topicsRootName + "-outbound");
    requiredTopics.add(topicsRootName + "-inbound.DLT");
    requiredTopics.removeAll(existedTopics);
    return requiredTopics;
  }

  private void customizeAdminClientMock(Set<String> topics) throws Exception {
    doReturn(topics).when(listTopicsFuture).get(anyLong(), any(TimeUnit.class));
    when(listTopicsResult.names()).thenReturn(listTopicsFuture);
    when(adminClient.listTopics()).thenReturn(listTopicsResult);
    when(createTopicsResult.all()).thenReturn(createTopicsFuture);
    when(adminClient.createTopics(anyCollection())).thenReturn(createTopicsResult);
  }

  private KafkaProperties createKafkaProperties(String topicsRootName) {
    var handler = new Handler();
    handler.setRequest(topicsRootName + "-inbound");
    handler.setReplay(topicsRootName + "-outbound");

    var topics = Map.of(topicsRootName, handler);

    KafkaProperties kafkaProperties = new KafkaProperties();
    kafkaProperties.setErrorHandler(new ErrorHandler());
    kafkaProperties.getErrorHandler().setMaxElapsedTime(5000L);
    kafkaProperties.setTopicProperties(new TopicProperties());
    kafkaProperties.getTopicProperties().setNumPartitions(1);
    kafkaProperties.getTopicProperties().setReplicationFactor((short) 1);
    kafkaProperties.setTopics(topics);

    var retentionPolicyInDays = new RetentionPolicyInDays();
    retentionPolicyInDays.setRead(RETENTION_DAYS_FOR_READ);
    retentionPolicyInDays.setWrite(RETENTION_DAYS_FOR_WRITE);
    kafkaProperties.getTopicProperties().setRetentionPolicyInDays(retentionPolicyInDays);

    return kafkaProperties;
  }
}
