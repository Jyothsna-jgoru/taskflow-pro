package com.taskflowpro.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;

@Entity
@Table(name = "projects")
public class Project {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id")
  private Workspace workspace;

  @Column(nullable = false, length = 160)
  private String name;

  @Column(length = 2000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProjectStatus status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id")
  private User owner;

  @Column(name = "start_date")
  private LocalDate startDate;

  @Column(name = "target_date")
  private LocalDate targetDate;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected Project() {}

  public Project(
      Workspace workspace,
      String name,
      String description,
      ProjectStatus status,
      User owner,
      LocalDate startDate,
      LocalDate targetDate) {
    this.workspace = workspace;
    this.name = name;
    this.description = description;
    this.status = status;
    this.owner = owner;
    this.startDate = startDate;
    this.targetDate = targetDate;
  }

  public UUID getId() {
    return id;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public ProjectStatus getStatus() {
    return status;
  }

  public User getOwner() {
    return owner;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getTargetDate() {
    return targetDate;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(
      String name,
      String description,
      ProjectStatus status,
      User owner,
      LocalDate startDate,
      LocalDate targetDate) {
    this.name = name;
    this.description = description;
    this.status = status;
    this.owner = owner;
    this.startDate = startDate;
    this.targetDate = targetDate;
    this.updatedAt = Instant.now();
  }

  public void archive() {
    this.status = ProjectStatus.ARCHIVED;
    this.updatedAt = Instant.now();
  }
}
