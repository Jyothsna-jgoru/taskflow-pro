package com.taskflowpro.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "activity_events")
public class ActivityEvent {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id")
  private Workspace workspace;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id")
  private TaskItem task;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "actor_id")
  private User actor;

  @Enumerated(EnumType.STRING)
  @Column(name = "event_type", nullable = false, length = 40)
  private EventType eventType;

  @Column(nullable = false, length = 500)
  private String message;

  @Column(name = "metadata_json")
  private String metadataJson;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  protected ActivityEvent() {}

  public ActivityEvent(
      Workspace workspace,
      TaskItem task,
      User actor,
      EventType eventType,
      String message,
      String metadataJson) {
    this.workspace = workspace;
    this.task = task;
    this.actor = actor;
    this.eventType = eventType;
    this.message = message;
    this.metadataJson = metadataJson;
  }

  public UUID getId() {
    return id;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public TaskItem getTask() {
    return task;
  }

  public User getActor() {
    return actor;
  }

  public EventType getEventType() {
    return eventType;
  }

  public String getMessage() {
    return message;
  }

  public String getMetadataJson() {
    return metadataJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
