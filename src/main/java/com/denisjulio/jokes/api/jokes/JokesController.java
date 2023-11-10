package com.denisjulio.jokes.api.jokes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            .orElseThrow();
  }

  @DeleteMapping("/{jokeId}")
  public ResponseEntity<Object> deleteJokeById(@PathVariable("jokeId") Long jokeId) {
    return jokesService.getJokeById(jokeId)
            .map(joke -> {
              jokesService.deleteJoke(joke);
              return ResponseEntity.noContent().build();
            })
            .orElseThrow();
  }
}
