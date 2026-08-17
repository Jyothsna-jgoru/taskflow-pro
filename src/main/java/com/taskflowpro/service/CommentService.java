package com.taskflowpro.service;

import com.taskflowpro.dto.TaskDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.CommentRepository;
import com.taskflowpro.security.CurrentUser;
import java.util.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {
  private final CommentRepository comments;
  private final TaskService taskService;
  private final CurrentUser currentUser;
  private final MembershipGuard guard;
  private final ActivityService activity;
  private final ApiMapper mapper;

  public CommentService(
      CommentRepository comments,
      TaskService taskService,
      CurrentUser currentUser,
      MembershipGuard guard,
      ActivityService activity,
      ApiMapper mapper) {
    this.comments = comments;
    this.taskService = taskService;
    this.currentUser = currentUser;
    this.guard = guard;
    this.activity = activity;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public List<CommentResponse> list(UUID workspaceId, UUID taskId) {
    User user = currentUser.require();
    guard.require(workspaceId, user.getId());
    taskService.require(workspaceId, taskId);
    return comments.findAllByTaskIdOrderByCreatedAtAsc(taskId).stream()
        .map(mapper::comment)
        .toList();
  }

  @Transactional
  @CacheEvict(cacheNames = "dashboard", allEntries = true)
  public CommentResponse add(UUID workspaceId, UUID taskId, CommentRequest request) {
    User user = currentUser.require();
    guard.require(workspaceId, user.getId());
    TaskItem task = taskService.require(workspaceId, taskId);
    Comment comment = comments.save(new Comment(task, user, request.body().trim()));
    activity.record(
        task.getWorkspace(),
        task,
        user,
        EventType.COMMENT_ADDED,
        user.getDisplayName() + " added a comment");
    return mapper.comment(comment);
  }
}
