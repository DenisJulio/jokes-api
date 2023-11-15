package com.denisjulio.jokes.api.utils;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "keycloak-client")
public record KeycloakClientProperties(Client client, List<User> users) {

  public record Client(String id, String secret) {
  }

  public record User(String username, String password) {
  }
}
