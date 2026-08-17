package com.taskflowpro.service;

import com.taskflowpro.dto.WorkspaceDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.exception.*;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.*;
import com.taskflowpro.security.CurrentUser;
import java.util.*;
import org.springframework.cache.annotation.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {
  private final WorkspaceRepository workspaces;
  private final WorkspaceMemberRepository members;
  private final UserRepository users;
  private final CurrentUser currentUser;
  private final MembershipGuard guard;
  private final ActivityService activity;
  private final ApiMapper mapper;

  public WorkspaceService(
      WorkspaceRepository workspaces,
      WorkspaceMemberRepository members,
      UserRepository users,
      CurrentUser currentUser,
      MembershipGuard guard,
      ActivityService activity,
      ApiMapper mapper) {
    this.workspaces = workspaces;
    this.members = members;
    this.users = users;
    this.currentUser = currentUser;
    this.guard = guard;
    this.activity = activity;
    this.mapper = mapper;
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public WorkspaceResponse create(WorkspaceRequest request) {
    User user = currentUser.require();
    Workspace workspace =
        workspaces.save(new Workspace(request.name().trim(), request.description(), user));
    WorkspaceMember membership =
        members.save(new WorkspaceMember(workspace, user, WorkspaceRole.ADMIN));
    activity.record(
        workspace,
        null,
        user,
        EventType.WORKSPACE_CREATED,
        user.getDisplayName() + " created the workspace");
    return response(workspace, membership.getRole(), 1);
  }

  @Transactional(readOnly = true)
  public List<WorkspaceResponse> list() {
    User user = currentUser.require();
    return members.findMembershipsWithWorkspace(user.getId()).stream()
        .map(
            m ->
                response(
                    m.getWorkspace(),
                    m.getRole(),
                    members
                        .findAllByWorkspaceIdOrderByJoinedAtAsc(m.getWorkspace().getId())
                        .size()))
        .toList();
  }

  @Transactional(readOnly = true)
  public WorkspaceResponse get(UUID workspaceId) {
    User user = currentUser.require();
    WorkspaceMember membership = guard.require(workspaceId, user.getId());
    Workspace workspace =
        workspaces
            .findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));
    return response(
        workspace,
        membership.getRole(),
        members.findAllByWorkspaceIdOrderByJoinedAtAsc(workspaceId).size());
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public WorkspaceResponse update(UUID workspaceId, WorkspaceRequest request) {
    User user = currentUser.require();
    guard.requireAny(workspaceId, user.getId(), WorkspaceRole.ADMIN);
    Workspace workspace =
        workspaces
            .findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));
    workspace.update(request.name().trim(), request.description());
    return response(
        workspace,
        WorkspaceRole.ADMIN,
        members.findAllByWorkspaceIdOrderByJoinedAtAsc(workspaceId).size());
  }

  @Transactional(readOnly = true)
  public List<MemberResponse> listMembers(UUID workspaceId) {
    User user = currentUser.require();
    guard.require(workspaceId, user.getId());
    return members.findAllByWorkspaceIdOrderByJoinedAtAsc(workspaceId).stream()
        .map(mapper::member)
        .toList();
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public MemberResponse addMember(UUID workspaceId, MemberRequest request) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN);
    User user =
        users
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new NotFoundException("No registered user has that email"));
    if (members.existsByWorkspaceIdAndUserId(workspaceId, user.getId()))
      throw new ConflictException("User is already a workspace member");
    Workspace workspace =
        workspaces
            .findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));
    return mapper.member(members.save(new WorkspaceMember(workspace, user, request.role())));
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public MemberResponse updateRole(UUID workspaceId, UUID membershipId, RoleRequest request) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN);
    WorkspaceMember target = memberInWorkspace(workspaceId, membershipId);
    ensureAdminRemains(workspaceId, target, request.role());
    target.setRole(request.role());
    return mapper.member(target);
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public void removeMember(UUID workspaceId, UUID membershipId) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN);
    WorkspaceMember target = memberInWorkspace(workspaceId, membershipId);
    ensureAdminRemains(workspaceId, target, WorkspaceRole.MEMBER);
    members.delete(target);
  }

  private WorkspaceMember memberInWorkspace(UUID workspaceId, UUID membershipId) {
    WorkspaceMember value =
        members
            .findById(membershipId)
            .orElseThrow(() -> new NotFoundException("Membership not found"));
    if (!value.getWorkspace().getId().equals(workspaceId))
      throw new NotFoundException("Membership not found");
    return value;
  }

  private void ensureAdminRemains(UUID workspaceId, WorkspaceMember target, WorkspaceRole newRole) {
    if (target.getRole() == WorkspaceRole.ADMIN
        && newRole != WorkspaceRole.ADMIN
        && members.countByWorkspaceIdAndRole(workspaceId, WorkspaceRole.ADMIN) <= 1)
      throw new ConflictException("A workspace must keep at least one admin");
  }

  private WorkspaceResponse response(Workspace w, WorkspaceRole role, int count) {
    return new WorkspaceResponse(
        w.getId(),
        w.getName(),
        w.getDescription(),
        role,
        count,
        w.getCreatedAt(),
        w.getUpdatedAt());
  }
}
