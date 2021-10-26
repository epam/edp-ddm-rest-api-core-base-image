package com.epam.digital.data.platform.restapi.core.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfigProperties {
  private String url;
  private List<String> realms;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public List<String> getRealms() {
    return realms;
  }

  public void setRealms(List<String> realms) {
    this.realms = realms;
  }
}
