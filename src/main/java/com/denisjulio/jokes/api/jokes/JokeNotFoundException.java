package com.denisjulio.jokes.api.jokes;

public class JokeNotFoundException extends RuntimeException {

  public JokeNotFoundException(Long id) {
    super("Could not find joke " + id);
  }
}
