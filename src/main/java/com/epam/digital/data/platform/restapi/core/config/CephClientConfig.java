package com.epam.digital.data.platform.restapi.core.config;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import com.epam.digital.data.platform.integration.ceph.service.impl.CephServiceS3Impl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CephClientConfig {

  @Bean
  public CephService lowcodeCephService(
      @Value("${ceph.http-endpoint}") String uri,
      @Value("${ceph.access-key}") String accessKey,
      @Value("${ceph.secret-key}") String secretKey
  ) {
    return new CephServiceS3Impl(cephAmazonS3(uri, accessKey, secretKey));
  }

  @Bean
  public CephService lowcodeFileCephService(
      @Value("${lowcode-file-ceph.http-endpoint}") String uri,
      @Value("${lowcode-file-ceph.access-key}") String accessKey,
      @Value("${lowcode-file-ceph.secret-key}") String secretKey
  ) {
    return new CephServiceS3Impl(cephAmazonS3(uri, accessKey, secretKey));
  }

  @Bean
  public CephService datafactoryCephService(
      @Value("${datafactoryceph.http-endpoint}") String uri,
      @Value("${datafactoryceph.access-key}") String accessKey,
      @Value("${datafactoryceph.secret-key}") String secretKey
  ) {
    return new CephServiceS3Impl(cephAmazonS3(uri, accessKey, secretKey));
  }

  @Bean
  public CephService datafactoryResponseCephService(
      @Value("${datafactory-response-ceph.http-endpoint}") String uri,
      @Value("${datafactory-response-ceph.access-key}") String accessKey,
      @Value("${datafactory-response-ceph.secret-key}") String secretKey) {
    return new CephServiceS3Impl(cephAmazonS3(uri, accessKey, secretKey));
  }

  @Bean
  public CephService datafactoryFileCephService(
          @Value("${datafactory-file-ceph.http-endpoint}") String uri,
          @Value("${datafactory-file-ceph.access-key}") String accessKey,
          @Value("${datafactory-file-ceph.secret-key}") String secretKey) {
    return new CephServiceS3Impl(cephAmazonS3(uri, accessKey, secretKey));
  }

  private AmazonS3 cephAmazonS3(String uri, String accessKey, String secretKey) {

    var credentials = new AWSStaticCredentialsProvider(
        new BasicAWSCredentials(accessKey, secretKey));

    var clientConfig = new ClientConfiguration();
    clientConfig.setProtocol(Protocol.HTTP);

    return AmazonS3ClientBuilder.standard()
        .withCredentials(credentials)
        .withClientConfiguration(clientConfig)
        .withEndpointConfiguration(new EndpointConfiguration(uri, null))
        .withPathStyleAccessEnabled(true)
        .build();
  }
}
