package com.denisjulio.jokes.api.jokes;

import jakarta.persistence.*;

@Table(name = "joke")
@Entity
public class Joke {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  private Long id;

  @Column(name = "content", nullable = false)
  private String content;

  @Column(name = "author-id", nullable = false)
  private String authorId;

  public Joke() {}

  public Joke(String content, String authorId) {
    this.content = content;
    this.authorId = authorId;
  }

  public Long getId() {
    return id;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getAuthorId() {
    return authorId;
  }

  public void setAuthorId(String authorId) {
    this.authorId = authorId;
  }
}
