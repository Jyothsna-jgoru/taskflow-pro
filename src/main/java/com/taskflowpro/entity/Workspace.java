package com.taskflowpro.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class Workspace {
  @Id private UUID id = UUID.randomUUID();

  @Column(nullable = false, length = 120)
  private String name;

  @Column(length = 500)
  private String description;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Workspace() {}

  public Workspace(String name, String description, User createdBy) {
    this.name = name;
    this.description = description;
    this.createdBy = createdBy;
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(String name, String description) {
    this.name = name;
    this.description = description;
    this.updatedAt = Instant.now();
  }
}
