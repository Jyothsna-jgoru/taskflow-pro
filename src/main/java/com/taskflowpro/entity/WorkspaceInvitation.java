package com.taskflowpro.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "workspace_invitations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "email"}))
public class WorkspaceInvitation {
  @Id private UUID id = UUID.randomUUID();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "workspace_id")
  private Workspace workspace;

  @Column(nullable = false, length = 254)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private WorkspaceRole role;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "invited_by")
  private User invitedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "token_hash", length = 64, unique = true)
  private String tokenHash;

  protected WorkspaceInvitation() {}

  public WorkspaceInvitation(
      Workspace workspace,
      String email,
      WorkspaceRole role,
      User invitedBy,
      Instant expiresAt,
      String tokenHash) {
    this.workspace = workspace;
    this.email = email.toLowerCase().trim();
    this.role = role;
    this.invitedBy = invitedBy;
    this.expiresAt = expiresAt;
    this.tokenHash = tokenHash;
  }

  public UUID getId() {
    return id;
  }

  public Workspace getWorkspace() {
    return workspace;
  }

  public String getEmail() {
    return email;
  }

  public WorkspaceRole getRole() {
    return role;
  }

  public User getInvitedBy() {
    return invitedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getExpiresAt() {
    return expiresAt;
  }

  public String getTokenHash() {
    return tokenHash;
  }

  public boolean isExpired(Instant now) {
    return !expiresAt.isAfter(now);
  }

  public void renew(WorkspaceRole role, User invitedBy, Instant expiresAt, String tokenHash) {
    this.role = role;
    this.invitedBy = invitedBy;
    this.expiresAt = expiresAt;
    this.tokenHash = tokenHash;
  }
}
