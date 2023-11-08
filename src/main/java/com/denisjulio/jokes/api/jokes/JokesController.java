package com.denisjulio.jokes.api.jokes;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController("/jokes")
public class JokesController {

  private final JokesRepository jokesRepository;

  public JokesController(JokesRepository jokesRepository) {
    this.jokesRepository = jokesRepository;
  }


  @GetMapping
  public ResponseEntity<List<Joke>> getJokes() {
    var jokes = jokesRepository.findAll();
    return ResponseEntity.ok(jokes);
  }

  @PostMapping
  public ResponseEntity<Joke> postNewJoke(@RequestBody JokeSubmissionData jokeData, @AuthenticationPrincipal Jwt jwt) {
    var newJoke = new Joke(jokeData.getContent(), jwt.getSubject());
    return ResponseEntity
            .status(201)
            .body(jokesRepository.save(newJoke));
  }

  @GetMapping("/{jokeId}")
  public ResponseEntity<Joke> getJokeById(@PathVariable("jokeId") Long jokeId) {
    var joke = jokesRepository.findById(jokeId)
            .orElseThrow();
    return ResponseEntity.ok(joke);
  }

  @DeleteMapping("/{jokeId}")
  public ResponseEntity<Void> deleteJokeById(@PathVariable("jokeId") Long jokeId) {
    jokesRepository.deleteById(jokeId);
    return ResponseEntity.status(204).build();
  }
}
