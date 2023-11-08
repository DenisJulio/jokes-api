package com.denisjulio.jokes.api.jokes;

import org.springframework.data.jpa.repository.JpaRepository;

public interface JokesRepository extends JpaRepository<Joke, Long> {
}