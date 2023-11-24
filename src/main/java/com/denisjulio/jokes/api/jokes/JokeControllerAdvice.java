package com.denisjulio.jokes.api.jokes;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class JokeControllerAdvice {
    
  @ExceptionHandler
  ResponseEntity<Object> jokeNotFoundHandler(JokeNotFoundException ex) {
    return ResponseEntity.notFound().build();
  }
}
