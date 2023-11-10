package com.denisjulio.jokes.api;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc()
class SecurityTest {

  @Autowired
  private MockMvc mvc;

  private String jokeSubmissionData;
  private Jwt validJwt;

  @BeforeEach
  void setup() {
    jokeSubmissionData = """
            {
              "content": "A new Joke"
            }
            """;
    validJwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("user")
            .claim("scope", "joker")
            .build();
  }

  @Test
  void whenPostNewJokeWithValidJwtThenReturnCreated() throws Exception {
    var jsonRes = mvc.perform(post("/jokes")
                    .content(jokeSubmissionData)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(jwt().jwt(validJwt)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.authorId").value(is(validJwt.getSubject())))
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
  void whenDeleteJokeByIdWithInsufficientAuthoritiesThenReturn403() throws Exception {
    var jsonRes = mvc.perform(post("/jokes")
                    .content(jokeSubmissionData)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(jwt().jwt(validJwt)))
            .andReturn().getResponse().getContentAsString();
    var json = new JSONObject(jsonRes);
    mvc.perform(delete("/jokes/{jokeId}", json.get("id"))
                    .with(jwt().jwt(validJwt)))
            .andExpect(status().isNoContent());
  }

  @Test
  void whenRequestJokesThenReturnSuccessful() throws Exception {
    mvc.perform(get("/jokes")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray());
  }

}
