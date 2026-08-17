package com.taskflowpro.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {
  @Id private UUID id = UUID.randomUUID();

  @Column(nullable = false, unique = true, length = 254)
  private String email;

  @Column(name = "password_hash", nullable = false, length = 100)
  private String passwordHash;

  @Column(name = "display_name", nullable = false, length = 100)
  private String displayName;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected User() {}

  public User(String email, String passwordHash, String displayName) {
    this.email = email.toLowerCase().trim();
    this.passwordHash = passwordHash;
    this.displayName = displayName.trim();
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public String getDisplayName() {
    return displayName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
}
