package com.taskflowpro.service;

import com.taskflowpro.dto.TaskDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.*;
import com.taskflowpro.security.CurrentUser;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskDetailService {
  private final TaskService tasks;
  private final CommentRepository comments;
  private final ActivityEventRepository events;
  private final CurrentUser currentUser;
  private final MembershipGuard guard;
  private final ApiMapper mapper;

  public TaskDetailService(
      TaskService tasks,
      CommentRepository comments,
      ActivityEventRepository events,
      CurrentUser currentUser,
      MembershipGuard guard,
      ApiMapper mapper) {
    this.tasks = tasks;
    this.comments = comments;
    this.events = events;
    this.currentUser = currentUser;
    this.guard = guard;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public TaskDetailResponse get(UUID workspaceId, UUID taskId) {
    User user = currentUser.require();
    guard.require(workspaceId, user.getId());
    TaskItem task = tasks.require(workspaceId, taskId);
    return new TaskDetailResponse(
        mapper.task(task),
        comments.findAllByTaskIdOrderByCreatedAtAsc(taskId).stream().map(mapper::comment).toList(),
        events.findAllByTaskIdOrderByCreatedAtDesc(taskId).stream().map(mapper::activity).toList());
  }
}
