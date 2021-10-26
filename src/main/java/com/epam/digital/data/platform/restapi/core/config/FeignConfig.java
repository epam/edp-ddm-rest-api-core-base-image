package com.epam.digital.data.platform.restapi.core.config;

import com.epam.digital.data.platform.restapi.core.service.KeycloakRestClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(clients = {KeycloakRestClient.class})
public class FeignConfig {
}
