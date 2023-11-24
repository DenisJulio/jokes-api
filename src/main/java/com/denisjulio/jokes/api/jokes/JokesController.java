package com.denisjulio.jokes.api.jokes;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController()
@RequestMapping("/jokes")
public class JokesController {

  private final JokesService jokesService;

  public JokesController(JokesService jokesService) {
    this.jokesService = jokesService;
  }

  @GetMapping
  public ResponseEntity<List<Joke>> getJokes() {
    var jokes = jokesService.getJokes();
    return ResponseEntity.ok(jokes);
  }

  @PostMapping
  public ResponseEntity<Joke> postNewJoke(@RequestBody JokeSubmissionData jokeData, @AuthenticationPrincipal Jwt jwt) {
    var newJoke = new Joke(jokeData.getContent(), jwt.getSubject());
    return ResponseEntity
        .status(201)
        .body(jokesService.saveJoke(newJoke));
  }

  @GetMapping("/{jokeId}")
  public ResponseEntity<Joke> getJokeById(@PathVariable(value = "jokeId") Long jokeId) {
    return jokesService.getJokeById(jokeId)
        .map(ResponseEntity::ok)
        .orElseThrow(() -> new JokeNotFoundException(jokeId));
  }

  @DeleteMapping("/{jokeId}")
  public ResponseEntity<Object> deleteJokeById(@PathVariable("jokeId") Long jokeId) {
    return jokesService.getJokeById(jokeId)
        .map(joke -> {
          jokesService.deleteJoke(joke);
          return ResponseEntity.noContent().build();
        })
        .orElseThrow(() -> new JokeNotFoundException(jokeId));
  }
}
