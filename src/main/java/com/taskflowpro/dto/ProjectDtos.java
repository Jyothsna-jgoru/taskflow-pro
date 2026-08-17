package com.taskflowpro.dto;

import com.taskflowpro.entity.ProjectStatus;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.UUID;

public final class ProjectDtos {
  private ProjectDtos() {}

  public record ProjectRequest(
      @NotBlank @Size(max = 160) String name,
      @Size(max = 2000) String description,
      @NotNull ProjectStatus status,
      UUID ownerId,
      LocalDate startDate,
      LocalDate targetDate) {}

  public record ProjectResponse(
      UUID id,
      UUID workspaceId,
      String name,
      String description,
      ProjectStatus status,
      AuthDtos.UserResponse owner,
      LocalDate startDate,
      LocalDate targetDate,
      long totalTasks,
      long completedTasks,
      int progressPercentage,
      Instant createdAt,
      Instant updatedAt) {}
}
