package com.epam.digital.data.platform.restapi.core.config.properties;

import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("data-platform.kafka")
public class KafkaProperties {

  private String bootstrap;
  private String groupId;
  private TopicProperties topicProperties;
  private Map<String, Handler> topics;
  private ErrorHandler errorHandler = new ErrorHandler();
  private List<String> trustedPackages;
  private SslProperties ssl;

  public List<String> getTrustedPackages() {
    return trustedPackages;
  }

  public void setTrustedPackages(List<String> trustedPackages) {
    this.trustedPackages = trustedPackages;
  }

  public String getBootstrap() {
    return bootstrap;
  }

  public void setBootstrap(String bootstrap) {
    this.bootstrap = bootstrap;
  }

  public ErrorHandler getErrorHandler() {
    return errorHandler;
  }

  public void setErrorHandler(ErrorHandler errorHandler) {
    this.errorHandler = errorHandler;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public TopicProperties getTopicProperties() {
    return topicProperties;
  }

  public void setTopicProperties(TopicProperties topicProperties) {
    this.topicProperties = topicProperties;
  }

  public Map<String, Handler> getTopics() {
    return topics;
  }

  public void setTopics(
      Map<String, Handler> topics) {
    this.topics = topics;
  }

  public SslProperties getSsl() {
    return ssl;
  }

  public void setSsl(
      SslProperties ssl) {
    this.ssl = ssl;
  }

  public static class Handler {

    private String request;
    private String replay;

    public String getRequest() {
      return request;
    }

    public void setRequest(String request) {
      this.request = request;
    }

    public String getReplay() {
      return replay;
    }

    public void setReplay(String replay) {
      this.replay = replay;
    }
  }

  public static class ErrorHandler {

    private Long initialInterval;
    private Long maxElapsedTime;
    private Double multiplier;

    public Long getInitialInterval() {
      return initialInterval;
    }

    public void setInitialInterval(Long initialInterval) {
      this.initialInterval = initialInterval;
    }

    public Long getMaxElapsedTime() {
      return maxElapsedTime;
    }

    public void setMaxElapsedTime(Long maxElapsedTime) {
      this.maxElapsedTime = maxElapsedTime;
    }

    public Double getMultiplier() {
      return multiplier;
    }

    public void setMultiplier(Double multiplier) {
      this.multiplier = multiplier;
    }
  }

  public static class TopicProperties {

    private Integer numPartitions;
    private Short replicationFactor;
    private RetentionPolicyInDays retentionPolicyInDays;

    public Integer getNumPartitions() {
      return numPartitions;
    }

    public void setNumPartitions(Integer numPartitions) {
      this.numPartitions = numPartitions;
    }

    public Short getReplicationFactor() {
      return replicationFactor;
    }

    public void setReplicationFactor(Short replicationFactor) {
      this.replicationFactor = replicationFactor;
    }

    public RetentionPolicyInDays getRetentionPolicyInDays() {
      return retentionPolicyInDays;
    }

    public void setRetentionPolicyInDays(RetentionPolicyInDays retentionPolicyInDays) {
      this.retentionPolicyInDays = retentionPolicyInDays;
    }
  }

  public static class RetentionPolicyInDays {

    private Integer read;
    private Integer write;

    public Integer getRead() {
      return read;
    }

    public void setRead(Integer read) {
      this.read = read;
    }

    public Integer getWrite() {
      return write;
    }

    public void setWrite(Integer write) {
      this.write = write;
    }
  }

  public static class SslProperties {

    private Boolean enabled;
    private String keystoreKey;
    private String keystoreCertificate;
    private String truststoreCertificate;

    public Boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(Boolean enabled) {
      this.enabled = enabled;
    }

    public String getKeystoreKey() {
      return keystoreKey;
    }

    public void setKeystoreKey(String keystoreKey) {
      this.keystoreKey = keystoreKey;
    }

    public String getKeystoreCertificate() {
      return keystoreCertificate;
    }

    public void setKeystoreCertificate(String keystoreCertificate) {
      this.keystoreCertificate = keystoreCertificate;
    }

    public String getTruststoreCertificate() {
      return truststoreCertificate;
    }

    public void setTruststoreCertificate(String truststoreCertificate) {
      this.truststoreCertificate = truststoreCertificate;
    }
  }
}
