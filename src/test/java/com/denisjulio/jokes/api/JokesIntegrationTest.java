package com.denisjulio.jokes.api;

import java.io.IOException;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import org.dmfs.httpessentials.exceptions.ProtocolError;
import org.dmfs.httpessentials.exceptions.ProtocolException;
import org.dmfs.httpessentials.httpurlconnection.HttpUrlConnectionExecutor;
import org.dmfs.oauth2.client.BasicOAuth2AuthorizationProvider;
import org.dmfs.oauth2.client.BasicOAuth2Client;
import org.dmfs.oauth2.client.BasicOAuth2ClientCredentials;
import org.dmfs.oauth2.client.OAuth2AccessToken;
import org.dmfs.oauth2.client.OAuth2Client;
import org.dmfs.oauth2.client.grants.ResourceOwnerPasswordGrant;
import org.dmfs.oauth2.client.scope.BasicScope;
import org.dmfs.rfc3986.encoding.Precoded;
import org.dmfs.rfc3986.uris.LazyUri;
import org.dmfs.rfc5545.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.denisjulio.jokes.api.jokes.Joke;
import com.denisjulio.jokes.api.utils.KeycloakClientProperties;
import com.denisjulio.jokes.api.utils.KeycloakClientProperties.User;

import dasniko.testcontainers.keycloak.KeycloakContainer;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class JokesIntegrationTest {

  private static final String KEYCLOAK_ADMIN = "admin";
  private static final String KEYCLOAK_ADMIN_PASSWORD = "admin";

  private static HttpUrlConnectionExecutor httpExecutor;
  private static Keycloak keycloakAdmin;

  private OAuth2Client oauthClient;

  @Autowired
  private KeycloakClientProperties clientProps;

  private String newJokeJson;

  @Container
  private static final KeycloakContainer keycloakContainer = new KeycloakContainer(
      "quay.io/keycloak/keycloak:22.0.1")
      .withRealmImportFile("realm-config/jokes-realm.json");

  @Container
  @ServiceConnection
  private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @DynamicPropertySource
  private static void propertyOverride(DynamicPropertyRegistry registry) {
    registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri",
        () -> keycloakContainer.getAuthServerUrl() + "/realms/jokes");
    registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        () -> keycloakContainer.getAuthServerUrl() + "/realms/jokes/protocol/openid-connect/certs");
  }

  @BeforeAll
  static void setupBase() {
    keycloakAdmin = Keycloak.getInstance(
        keycloakContainer.getAuthServerUrl(),
        "master",
        KEYCLOAK_ADMIN,
        KEYCLOAK_ADMIN_PASSWORD,
        "admin-cli");
    httpExecutor = new HttpUrlConnectionExecutor();
  }

  @BeforeEach
  void setup() {
    String issuerUri = keycloakContainer.getAuthServerUrl() + "/realms/jokes";
    var oauth2Provider = new BasicOAuth2AuthorizationProvider(
        URI.create(issuerUri + "/protocol/openid-connect/auth"),
        URI.create(issuerUri + "/protocol/openid-connect/token"),
        new Duration(1, 1));
    var credentials = new BasicOAuth2ClientCredentials(clientProps.client().id(), clientProps.client().secret());
    oauthClient = new BasicOAuth2Client(
        oauth2Provider,
        credentials,
        new LazyUri(new Precoded("http://localhost")));
    newJokeJson = """
        {
          "content": "A funny joke"
        }
        """;
  }

  @Test
  @Order(1)
  void whenLookingForJokesRealmInKeycloakThenReturnTrue() {
    assertThat(
        keycloakAdmin.realms().findAll()
            .stream()
            .anyMatch(realm -> realm.getRealm().equals("jokes")))
        .isTrue();
  }

  @Test
  @Order(2)
  void whenPostNewJokeFromAnAuthorizedUserThenReturnIsCreated(@Autowired WebTestClient webTestClient)
      throws ProtocolException, IOException, ProtocolError {
    var jokesApiUser = clientProps.users().getFirst();
    var token = getAccessToken(jokesApiUser);

    webTestClient.post().uri("/jokes")
        .body(BodyInserters.fromValue(newJokeJson))
        .header("Authorization", "Bearer " + token.accessToken())
        .header("content-type", "application/json")
        .exchange()
        .expectStatus().isCreated();
  }

@Test
@Order(3)
void whenDeleteJokeRequestedByDifferentAuthorThenReturnForbidden(@Autowired WebTestClient webTestClient)
        throws ProtocolException, IOException, ProtocolError {
    var anotherUser = (KeycloakClientProperties.User) clientProps.users().getLast();
    var token = getAccessToken(anotherUser);

    var postedJoke = webTestClient.get().uri("/jokes")
            .exchange()
            .expectStatus().isOk()
            .returnResult(Joke.class)
            .getResponseBody().blockFirst();

    assertThat(postedJoke).isNotNull();

    webTestClient.delete().uri("jokes/{jokeId}", postedJoke.getId())
            .header("Authorization", "Bearer " + token.accessToken())
            .exchange()
            .expectStatus()
            .isForbidden();
}

@Test
@Order(4)
void whenDeleteJokeRequestedByItsAuthorThenReturnNoContent(@Autowired WebTestClient webTestClient)
      throws ProtocolException, IOException, ProtocolError {
    var jokeAuthor = clientProps.users().getFirst();
    var token = getAccessToken(jokeAuthor);
    var postedJoke = webTestClient.get().uri("/jokes")
        .exchange()
        .expectStatus().isOk()
        .returnResult(Joke.class)
        .getResponseBody().blockFirst();
        
    assertThat(postedJoke).isNotNull();
    
    webTestClient.delete().uri("jokes/{jokeId}", postedJoke.getId())
        .header("Authorization", "Bearer " + token.accessToken())
        .exchange()
        .expectStatus()
        .isNoContent();
  }

  private OAuth2AccessToken getAccessToken(User jokesApiUser) throws ProtocolException, IOException, ProtocolError {
    return new ResourceOwnerPasswordGrant(
        oauthClient,
        new BasicScope("joker"),
        jokesApiUser.username(), jokesApiUser.password()).accessToken(httpExecutor);
  }
}
