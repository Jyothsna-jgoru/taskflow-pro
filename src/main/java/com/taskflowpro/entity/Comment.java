package com.taskflowpro.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comments")
public class Comment {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "task_id")
  private TaskItem task;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "author_id")
  private User author;

  @Column(nullable = false, length = 4000)
  private String body;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Comment() {}

  public Comment(TaskItem task, User author, String body) {
    this.task = task;
    this.author = author;
    this.body = body;
  }

  public UUID getId() {
    return id;
  }

  public TaskItem getTask() {
    return task;
  }

  public User getAuthor() {
    return author;
  }

  public String getBody() {
    return body;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
