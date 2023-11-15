package com.denisjulio.jokes.api.jokes;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class JokesService {

  private final JokesRepository jokesRepository;

  public JokesService(JokesRepository jokesRepository) {
    this.jokesRepository = jokesRepository;
  }

  public List<Joke> getJokes() {
    return jokesRepository.findAll();
  }

  public Optional<Joke> getJokeById(Long jokeId) {
    return jokesRepository.findById(jokeId);
  }

  public Joke saveJoke(Joke joke) {
    return jokesRepository.save(joke);
  }

  @PreAuthorize("#joke.authorId == principal.claims['sub']")
  public void deleteJoke(Joke joke) {
    jokesRepository.delete(joke);
  }
}
