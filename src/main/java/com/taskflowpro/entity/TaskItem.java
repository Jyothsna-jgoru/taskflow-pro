package com.taskflowpro.entity;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "tasks")
public class TaskItem {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id")
  private Workspace workspace;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "project_id")
  private Project project;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(length = 5000)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 30)
  private TaskStatus status;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskPriority priority;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "assignee_id")
  private User assignee;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "reporter_id")
  private User reporter;

  @Column(name = "due_date")
  private LocalDate dueDate;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "task_labels", joinColumns = @JoinColumn(name = "task_id"))
  @Column(name = "label", length = 40)
  private Set<String> labels = new LinkedHashSet<>();

  @Version
  @Column(nullable = false)
  private long version;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  protected TaskItem() {}

  public TaskItem(
      Workspace workspace,
      Project project,
      String title,
      String description,
      TaskStatus status,
      TaskPriority priority,
      User assignee,
      User reporter,
      LocalDate dueDate,
      Set<String> labels) {
    this.workspace = workspace;
    this.project = project;
    this.title = title;
    this.description = description;
    this.status = status;
    this.priority = priority;
    this.assignee = assignee;
    this.reporter = reporter;
    this.dueDate = dueDate;
    setLabels(labels);
  }

  public UUID getId() {
    return id;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public Project getProject() {
    return project;
  }

  public String getTitle() {
    return title;
  }

  public String getDescription() {
    return description;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public TaskPriority getPriority() {
    return priority;
  }

  public User getAssignee() {
    return assignee;
  }

  public User getReporter() {
    return reporter;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public Set<String> getLabels() {
    return Collections.unmodifiableSet(labels);
  }

  public long getVersion() {
    return version;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void update(
      Project project,
      String title,
      String description,
      TaskStatus status,
      TaskPriority priority,
      User assignee,
      LocalDate dueDate,
      Set<String> labels) {
    this.project = project;
    this.title = title;
    this.description = description;
    this.status = status;
    this.priority = priority;
    this.assignee = assignee;
    this.dueDate = dueDate;
    setLabels(labels);
    this.updatedAt = Instant.now();
  }

  private void setLabels(Set<String> values) {
    this.labels.clear();
    if (values != null)
      values.stream()
          .filter(Objects::nonNull)
          .map(String::trim)
          .filter(v -> !v.isBlank())
          .limit(10)
          .forEach(this.labels::add);
  }
}
