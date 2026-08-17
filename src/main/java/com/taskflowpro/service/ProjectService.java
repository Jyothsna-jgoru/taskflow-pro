package com.taskflowpro.service;

import com.taskflowpro.dto.ProjectDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.exception.BadRequestException;
import com.taskflowpro.exception.NotFoundException;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.*;
import com.taskflowpro.security.CurrentUser;
import java.util.*;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
  private final ProjectRepository projects;
  private final WorkspaceRepository workspaces;
  private final UserRepository users;
  private final TaskRepository tasks;
  private final CurrentUser currentUser;
  private final MembershipGuard guard;
  private final ActivityService activity;
  private final ApiMapper mapper;

  public ProjectService(
      ProjectRepository projects,
      WorkspaceRepository workspaces,
      UserRepository users,
      TaskRepository tasks,
      CurrentUser currentUser,
      MembershipGuard guard,
      ActivityService activity,
      ApiMapper mapper) {
    this.projects = projects;
    this.workspaces = workspaces;
    this.users = users;
    this.tasks = tasks;
    this.currentUser = currentUser;
    this.guard = guard;
    this.activity = activity;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = "projects",
      key =
          "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication.name + ':' + #workspaceId")
  public List<ProjectResponse> list(UUID workspaceId) {
    User user = currentUser.require();
    guard.require(workspaceId, user.getId());
    return projects.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
        .map(this::response)
        .toList();
  }

  @Transactional(readOnly = true)
  public ProjectResponse get(UUID workspaceId, UUID projectId) {
    User user = currentUser.require();
    guard.require(workspaceId, user.getId());
    return response(require(workspaceId, projectId));
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "projects", allEntries = true),
        @CacheEvict(cacheNames = "dashboard", allEntries = true)
      })
  public ProjectResponse create(UUID workspaceId, ProjectRequest request) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN, WorkspaceRole.MANAGER);
    validateDates(request);
    Workspace workspace =
        workspaces
            .findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));
    User owner = resolveUser(workspaceId, request.ownerId());
    Project project =
        projects.save(
            new Project(
                workspace,
                request.name().trim(),
                request.description(),
                request.status(),
                owner,
                request.startDate(),
                request.targetDate()));
    activity.record(
        workspace,
        null,
        actor,
        EventType.PROJECT_CREATED,
        actor.getDisplayName() + " created project “" + project.getName() + "”");
    return response(project);
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "projects", allEntries = true),
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "tasks", allEntries = true)
      })
  public ProjectResponse update(UUID workspaceId, UUID projectId, ProjectRequest request) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN, WorkspaceRole.MANAGER);
    validateDates(request);
    Project project = require(workspaceId, projectId);
    project.update(
        request.name().trim(),
        request.description(),
        request.status(),
        resolveUser(workspaceId, request.ownerId()),
        request.startDate(),
        request.targetDate());
    activity.record(
        project.getWorkspace(),
        null,
        actor,
        EventType.PROJECT_UPDATED,
        actor.getDisplayName() + " updated project “" + project.getName() + "”");
    return response(project);
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "projects", allEntries = true),
        @CacheEvict(cacheNames = "dashboard", allEntries = true)
      })
  public ProjectResponse archive(UUID workspaceId, UUID projectId) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN, WorkspaceRole.MANAGER);
    Project project = require(workspaceId, projectId);
    project.archive();
    activity.record(
        project.getWorkspace(),
        null,
        actor,
        EventType.PROJECT_UPDATED,
        actor.getDisplayName() + " archived project “" + project.getName() + "”");
    return response(project);
  }

  public Project require(UUID workspaceId, UUID projectId) {
    Project project =
        projects.findById(projectId).orElseThrow(() -> new NotFoundException("Project not found"));
    if (!project.getWorkspace().getId().equals(workspaceId))
      throw new NotFoundException("Project not found");
    return project;
  }

  private User resolveUser(UUID workspaceId, UUID userId) {
    if (userId == null) return null;
    guard.requireMemberUser(workspaceId, userId);
    return users.findById(userId).orElseThrow(() -> new NotFoundException("User not found"));
  }

  private void validateDates(ProjectRequest request) {
    if (request.startDate() != null
        && request.targetDate() != null
        && request.targetDate().isBefore(request.startDate())) {
      throw new BadRequestException("Target date cannot be before the start date");
    }
  }

  private ProjectResponse response(Project p) {
    long total = tasks.countByProjectId(p.getId()),
        done = tasks.countByProjectIdAndStatus(p.getId(), TaskStatus.DONE);
    int percent = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
    return new ProjectResponse(
        p.getId(),
        p.getWorkspace().getId(),
        p.getName(),
        p.getDescription(),
        p.getStatus(),
        mapper.user(p.getOwner()),
        p.getStartDate(),
        p.getTargetDate(),
        total,
        done,
        percent,
        p.getCreatedAt(),
        p.getUpdatedAt());
  }
}
