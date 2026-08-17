package com.taskflowpro.service;

import com.taskflowpro.dto.TaskDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.exception.*;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.*;
import com.taskflowpro.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.*;
import org.springframework.cache.annotation.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
  private static final Set<String> SORT_FIELDS =
      Set.of("createdAt", "updatedAt", "dueDate", "priority", "status", "title");
  private final TaskRepository tasks;
  private final ProjectService projectService;
  private final UserRepository users;
  private final CurrentUser currentUser;
  private final MembershipGuard guard;
  private final ActivityService activity;
  private final ApiMapper mapper;

  public TaskService(
      TaskRepository tasks,
      ProjectService projectService,
      UserRepository users,
      CurrentUser currentUser,
      MembershipGuard guard,
      ActivityService activity,
      ApiMapper mapper) {
    this.tasks = tasks;
    this.projectService = projectService;
    this.users = users;
    this.currentUser = currentUser;
    this.guard = guard;
    this.activity = activity;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = "tasks",
      key =
          "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication.name + ':' + #workspaceId + ':' + #status + ':' + #priority + ':' + #assigneeId + ':' + #projectId + ':' + #dueAfter + ':' + #dueBefore + ':' + #search + ':' + #page + ':' + #size + ':' + #sort + ':' + #direction")
  public TaskPageResponse list(
      UUID workspaceId,
      TaskStatus status,
      TaskPriority priority,
      UUID assigneeId,
      UUID projectId,
      LocalDate dueAfter,
      LocalDate dueBefore,
      String search,
      int page,
      int size,
      String sort,
      String direction) {
    User actor = currentUser.require();
    guard.require(workspaceId, actor.getId());
    String safeSort = SORT_FIELDS.contains(sort) ? sort : "updatedAt";
    Sort.Direction dir =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable =
        PageRequest.of(Math.max(0, page), Math.min(Math.max(size, 1), 100), Sort.by(dir, safeSort));
    Specification<TaskItem> spec =
        filters(workspaceId, status, priority, assigneeId, projectId, dueAfter, dueBefore, search);
    Page<TaskItem> result = tasks.findAll(spec, pageable);
    return new TaskPageResponse(
        result.getContent().stream().map(mapper::task).toList(),
        result.getNumber(),
        result.getSize(),
        result.getTotalElements(),
        result.getTotalPages(),
        result.isLast());
  }

  @Transactional(readOnly = true)
  public TaskResponse get(UUID workspaceId, UUID taskId) {
    User actor = currentUser.require();
    guard.require(workspaceId, actor.getId());
    return mapper.task(require(workspaceId, taskId));
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public TaskResponse create(UUID workspaceId, TaskCreateRequest request) {
    User actor = currentUser.require();
    guard.require(workspaceId, actor.getId());
    Project project = projectService.require(workspaceId, request.projectId());
    User assignee = resolveAssignee(workspaceId, request.assigneeId());
    TaskItem task =
        tasks.saveAndFlush(
            new TaskItem(
                project.getWorkspace(),
                project,
                request.title().trim(),
                request.description(),
                request.status(),
                request.priority(),
                assignee,
                actor,
                request.dueDate(),
                request.labels()));
    activity.record(
        project.getWorkspace(),
        task,
        actor,
        EventType.TASK_CREATED,
        actor.getDisplayName() + " created this task");
    if (assignee != null)
      activity.record(
          project.getWorkspace(),
          task,
          actor,
          EventType.TASK_ASSIGNED,
          actor.getDisplayName() + " assigned this task to " + assignee.getDisplayName());
    return mapper.task(task);
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public TaskResponse update(UUID workspaceId, UUID taskId, TaskUpdateRequest request) {
    User actor = currentUser.require();
    guard.require(workspaceId, actor.getId());
    TaskItem task = require(workspaceId, taskId);
    if (task.getVersion() != request.version())
      throw new ConflictException("This task changed since you loaded it. Refresh and retry.");
    Project project = projectService.require(workspaceId, request.projectId());
    User assignee = resolveAssignee(workspaceId, request.assigneeId());
    TaskStatus oldStatus = task.getStatus();
    TaskPriority oldPriority = task.getPriority();
    UUID oldAssignee = task.getAssignee() == null ? null : task.getAssignee().getId();
    task.update(
        project,
        request.title().trim(),
        request.description(),
        request.status(),
        request.priority(),
        assignee,
        request.dueDate(),
        request.labels());
    tasks.saveAndFlush(task);
    if (oldStatus != request.status())
      activity.record(
          task.getWorkspace(),
          task,
          actor,
          EventType.TASK_STATUS_CHANGED,
          actor.getDisplayName()
              + " moved the task from "
              + readable(oldStatus)
              + " to "
              + readable(request.status()));
    if (oldPriority != request.priority())
      activity.record(
          task.getWorkspace(),
          task,
          actor,
          EventType.TASK_PRIORITY_CHANGED,
          actor.getDisplayName()
              + " changed priority from "
              + oldPriority
              + " to "
              + request.priority());
    UUID newAssignee = assignee == null ? null : assignee.getId();
    if (!Objects.equals(oldAssignee, newAssignee))
      activity.record(
          task.getWorkspace(),
          task,
          actor,
          EventType.TASK_ASSIGNED,
          assignee == null
              ? actor.getDisplayName() + " unassigned the task"
              : actor.getDisplayName() + " assigned the task to " + assignee.getDisplayName());
    return mapper.task(task);
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public void delete(UUID workspaceId, UUID taskId) {
    User actor = currentUser.require();
    WorkspaceMember member = guard.require(workspaceId, actor.getId());
    TaskItem task = require(workspaceId, taskId);
    if (member.getRole() == WorkspaceRole.MEMBER
        && !task.getReporter().getId().equals(actor.getId()))
      throw new ForbiddenException("Members can only delete tasks they reported");
    tasks.delete(task);
  }

  public TaskItem require(UUID workspaceId, UUID taskId) {
    TaskItem task =
        tasks.findById(taskId).orElseThrow(() -> new NotFoundException("Task not found"));
    if (!task.getWorkspace().getId().equals(workspaceId))
      throw new NotFoundException("Task not found");
    return task;
  }

  private User resolveAssignee(UUID workspaceId, UUID userId) {
    if (userId == null) return null;
    guard.requireMemberUser(workspaceId, userId);
    return users.findById(userId).orElseThrow(() -> new NotFoundException("Assignee not found"));
  }

  private String readable(Enum<?> value) {
    return value.name().replace('_', ' ').toLowerCase(Locale.ROOT);
  }

  private Specification<TaskItem> filters(
      UUID workspaceId,
      TaskStatus status,
      TaskPriority priority,
      UUID assigneeId,
      UUID projectId,
      LocalDate after,
      LocalDate before,
      String search) {
    return (root, query, cb) -> {
      List<Predicate> values = new ArrayList<>();
      values.add(cb.equal(root.get("workspace").get("id"), workspaceId));
      if (status != null) values.add(cb.equal(root.get("status"), status));
      if (priority != null) values.add(cb.equal(root.get("priority"), priority));
      if (assigneeId != null) values.add(cb.equal(root.get("assignee").get("id"), assigneeId));
      if (projectId != null) values.add(cb.equal(root.get("project").get("id"), projectId));
      if (after != null) values.add(cb.greaterThanOrEqualTo(root.get("dueDate"), after));
      if (before != null) values.add(cb.lessThanOrEqualTo(root.get("dueDate"), before));
      if (search != null && !search.isBlank()) {
        String pattern = "%" + search.toLowerCase(Locale.ROOT).trim() + "%";
        values.add(
            cb.or(
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)));
      }
      return cb.and(values.toArray(Predicate[]::new));
    };
  }
}
