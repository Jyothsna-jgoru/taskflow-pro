package com.taskflowpro.mapper;

import com.taskflowpro.dto.*;
import com.taskflowpro.entity.*;
import org.springframework.stereotype.Component;

@Component
public class ApiMapper {
  public AuthDtos.UserResponse user(User value) {
    return value == null
        ? null
        : new AuthDtos.UserResponse(
            value.getId(), value.getDisplayName(), value.getEmail(), value.getCreatedAt());
  }

  public WorkspaceDtos.MemberResponse member(WorkspaceMember value) {
    return new WorkspaceDtos.MemberResponse(
        value.getId(),
        value.getUser().getId(),
        value.getUser().getDisplayName(),
        value.getUser().getEmail(),
        value.getRole(),
        value.getJoinedAt());
  }

  public TaskDtos.TaskResponse task(TaskItem value) {
    return new TaskDtos.TaskResponse(
        value.getId(),
        value.getWorkspace().getId(),
        value.getProject().getId(),
        value.getProject().getName(),
        value.getTitle(),
        value.getDescription(),
        value.getStatus(),
        value.getPriority(),
        user(value.getAssignee()),
        user(value.getReporter()),
        value.getDueDate(),
        value.getLabels(),
        value.getVersion(),
        value.getCreatedAt(),
        value.getUpdatedAt());
  }

  public TaskDtos.CommentResponse comment(Comment value) {
    return new TaskDtos.CommentResponse(
        value.getId(),
        user(value.getAuthor()),
        value.getBody(),
        value.getCreatedAt(),
        value.getUpdatedAt());
  }

  public TaskDtos.ActivityResponse activity(ActivityEvent value) {
    return new TaskDtos.ActivityResponse(
        value.getId(),
        value.getEventType(),
        value.getMessage(),
        user(value.getActor()),
        value.getCreatedAt());
  }
}
