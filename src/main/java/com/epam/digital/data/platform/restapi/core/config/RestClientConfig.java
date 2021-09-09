package com.epam.digital.data.platform.restapi.core.config;

import com.epam.digital.data.platform.dso.client.DigitalSealRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {DigitalSealRestClient.class})
public class RestClientConfig {
}
