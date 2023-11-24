package com.denisjulio.jokes.api;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc()
class JokesControllerSecurityTest {

  @Container
  @ServiceConnection
  private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

  @Autowired
  private MockMvc mvc;

  private String jokeSubmissionData;
  private List<Jwt> validJwts;

  @BeforeEach
  void setup() {
    jokeSubmissionData = """
        {
          "content": "A new Joke"
        }
        """;
    validJwts = new ArrayList<>();
    validJwts.add(Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("userOne")
        .claim("scope", "joker")
        .build());
    validJwts.add(Jwt.withTokenValue("token")
        .header("alg", "none")
        .subject("userTwo")
        .claim("scope", "joker")
        .build());

  }

  @Test
  void whenPostNewJokeWithValidJwtThenReturnCreated() throws Exception {
    var jsonRes = mvc.perform(post("/jokes")
        .content(jokeSubmissionData)
        .contentType(MediaType.APPLICATION_JSON)
        .with(jwt().jwt(validJwts.get(0))))
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(notNullValue()))
        .andExpect(jsonPath("$.authorId").value(is(validJwts.get(0).getSubject())))
        .andReturn().getResponse().getContentAsString();
    JSONAssert.assertEquals(jokeSubmissionData, jsonRes, JSONCompareMode.LENIENT);
  }

  @Test
  void whenPostNewJokeWithInsufficientAuthoritiesThenReturn403() throws Exception {
    var tknSub = "user";
    mvc.perform(post("/jokes")
        .content(jokeSubmissionData)
        .contentType(MediaType.APPLICATION_JSON)
        .with(jwt().jwt(jwt -> jwt.subject(tknSub))))
        .andExpect(status().is(403));
  }

  @Test
  void whenDeleteJokeByIdWithValidJwtAndBeingItsAuthorThenReturnNoContent() throws Exception {
    var jsonRes = mvc.perform(post("/jokes")
        .content(jokeSubmissionData)
        .contentType(MediaType.APPLICATION_JSON)
        .with(jwt().jwt(validJwts.get(0))))
        .andReturn().getResponse().getContentAsString();
    var json = new JSONObject(jsonRes);
    mvc.perform(delete("/jokes/{jokeId}", json.get("id"))
        .with(jwt().jwt(validJwts.get(0))))
        .andExpect(status().isNoContent());
  }

  @Test
  void whenDeleteJokeByIdFromAnotherAuthorThenReturn403() throws Exception {
    var jsonRes = mvc.perform(post("/jokes")
        .content(jokeSubmissionData)
        .contentType(MediaType.APPLICATION_JSON)
        .with(jwt().jwt(validJwts.get(0))))
        .andReturn().getResponse().getContentAsString();
    var json = new JSONObject(jsonRes);
    mvc.perform(delete("/jokes/{jokeId}", json.get("id"))
        .with(jwt().jwt(validJwts.get(1))))
        .andExpect(status().is(403));
  }

  @Test
  void whenRequestJokesThenReturnSuccessful() throws Exception {
    mvc.perform(get("/jokes")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray());
  }

  @Test
  void whenGetJokeByIdAndJokeExistsThenReturnSuccesful() throws Exception {
    var jsonRes = mvc.perform(post("/jokes")
        .content(jokeSubmissionData)
        .contentType(MediaType.APPLICATION_JSON)
        .with(jwt().jwt(validJwts.get(0))))
        .andReturn().getResponse().getContentAsString();
    var json = new JSONObject(jsonRes);

    mvc.perform(get("/jokes/{jokeId}", json.get("id"))
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id", is(json.get("id"))))
        .andExpect(jsonPath("$.authorId", is(json.get("authorId"))))
        .andExpect(jsonPath("$.content", is(json.get("content"))));
  }

  @Test
  void whenGetJokeByIdAndJokeDoesNotExistThenReturn404() throws Exception {
    mvc.perform(get("/jokes/{jokeId}", 999)
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound());
  }
}
