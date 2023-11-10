package com.denisjulio.jokes.api;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc()
class SecurityTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void whenPostNewJokeWithValidJwtThenReturnCreated() throws Exception {
    var authorities = new SimpleGrantedAuthority("SCOPE_joker");
    var tknSub = "user";
    var json = """
            {
              "content": "A new Joke"
            }
            """;
    var jsonRes = mvc.perform(post("/jokes")
                    .content(json)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(jwt()
                            .jwt(jwt -> jwt.subject(tknSub))
                            .authorities(authorities)))
            .andExpect(status().isCreated())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.authorId").value(is(tknSub)))
            .andReturn().getResponse().getContentAsString();
    JSONAssert.assertEquals(json, jsonRes, JSONCompareMode.LENIENT);
  }

  @Test

  void whenPostNewJokeWithInsufficientAuthoritiesThenReturn403() throws Exception {
    var tknSub = "user";
    var json = """
            {
              "content": "A new Joke"
            }
            """;
    mvc.perform(post("/jokes")
                    .content(json)
                    .contentType(MediaType.APPLICATION_JSON)
                    .with(jwt().jwt(jwt -> jwt.subject(tknSub))))
            .andExpect(status().is(403));
  }

  @Test
  void whenRequestJokesThenReturnSuccessful() throws Exception {
    mvc.perform(get("/jokes")
                    .accept(MediaType.APPLICATION_JSON))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray());
  }

}
