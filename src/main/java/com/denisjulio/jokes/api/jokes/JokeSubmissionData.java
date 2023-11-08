package com.denisjulio.jokes.api.jokes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class JokeSubmissionData {

  @NotNull
  @JsonProperty(value = "content", required = true)
  private String content;

  @JsonCreator
  public JokeSubmissionData(String content) {
    this.content = content;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }
}
