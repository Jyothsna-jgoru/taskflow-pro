package com.taskflowpro.dto;

import com.taskflowpro.entity.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;

public final class TaskDtos {
  private TaskDtos() {}

  public record TaskCreateRequest(
      @NotNull UUID projectId,
      @NotBlank @Size(max = 200) String title,
      @Size(max = 5000) String description,
      @NotNull TaskStatus status,
      @NotNull TaskPriority priority,
      UUID assigneeId,
      LocalDate dueDate,
      @Size(max = 10) Set<@Size(max = 40) String> labels) {}

  public record TaskUpdateRequest(
      @NotNull UUID projectId,
      @NotBlank @Size(max = 200) String title,
      @Size(max = 5000) String description,
      @NotNull TaskStatus status,
      @NotNull TaskPriority priority,
      UUID assigneeId,
      LocalDate dueDate,
      @Size(max = 10) Set<@Size(max = 40) String> labels,
      @NotNull @PositiveOrZero Long version) {}

  public record TaskResponse(
      UUID id,
      UUID workspaceId,
      UUID projectId,
      String projectName,
      String title,
      String description,
      TaskStatus status,
      TaskPriority priority,
      AuthDtos.UserResponse assignee,
      AuthDtos.UserResponse reporter,
      LocalDate dueDate,
      Set<String> labels,
      long version,
      Instant createdAt,
      Instant updatedAt) {}

  public record TaskPageResponse(
      List<TaskResponse> content,
      int page,
      int size,
      long totalElements,
      int totalPages,
      boolean last) {}

  public record CommentRequest(@NotBlank @Size(max = 4000) String body) {}

  public record CommentResponse(
      UUID id, AuthDtos.UserResponse author, String body, Instant createdAt, Instant updatedAt) {}

  public record ActivityResponse(
      UUID id, EventType type, String message, AuthDtos.UserResponse actor, Instant createdAt) {}

  public record TaskDetailResponse(
      TaskResponse task, List<CommentResponse> comments, List<ActivityResponse> activity) {}
}
