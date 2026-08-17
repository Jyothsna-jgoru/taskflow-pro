package com.taskflowpro.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "workspace_members",
    uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "user_id"}))
public class WorkspaceMember {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id")
  private Workspace workspace;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorkspaceRole role;

  @Column(name = "joined_at", nullable = false, updatable = false)
  private Instant joinedAt = Instant.now();

  protected WorkspaceMember() {}

  public WorkspaceMember(Workspace workspace, User user, WorkspaceRole role) {
    this.workspace = workspace;
    this.user = user;
    this.role = role;
  }

  public UUID getId() {
    return id;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public User getUser() {
    return user;
  }

  public WorkspaceRole getRole() {
    return role;
  }

  public Instant getJoinedAt() {
    return joinedAt;
  }

  public void setRole(WorkspaceRole role) {
    this.role = role;
  }
}
