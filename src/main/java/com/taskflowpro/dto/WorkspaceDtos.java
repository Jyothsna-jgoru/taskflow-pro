package com.taskflowpro.dto;

import com.taskflowpro.entity.WorkspaceRole;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class WorkspaceDtos {
  private WorkspaceDtos() {}

  public record WorkspaceRequest(
      @NotBlank @Size(max = 120) String name, @Size(max = 500) String description) {}

  public record WorkspaceResponse(
      UUID id,
      String name,
      String description,
      WorkspaceRole currentUserRole,
      int memberCount,
      Instant createdAt,
      Instant updatedAt) {}

  public record MemberRequest(@NotBlank @Email String email, @NotNull WorkspaceRole role) {}

  public record RoleRequest(@NotNull WorkspaceRole role) {}

  public record MemberResponse(
      UUID membershipId,
      UUID userId,
      String displayName,
      String email,
      WorkspaceRole role,
      Instant joinedAt) {}

  public record InvitationResponse(
      UUID invitationId,
      String email,
      WorkspaceRole role,
      AuthDtos.UserResponse invitedBy,
      Instant createdAt,
      Instant expiresAt) {}

  public record InviteOrAddResponse(
      String action, MemberResponse member, InvitationResponse invitation, String invitationToken) {}

  public record InvitationPreviewResponse(String email, String workspaceName, Instant expiresAt) {}
}
