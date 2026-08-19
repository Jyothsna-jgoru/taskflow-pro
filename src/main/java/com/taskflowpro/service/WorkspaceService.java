package com.taskflowpro.service;

import com.taskflowpro.dto.WorkspaceDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.exception.*;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.*;
import com.taskflowpro.security.CurrentUser;
import com.taskflowpro.security.InvitationTokenService;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.springframework.cache.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkspaceService {
  private static final Duration INVITATION_VALIDITY = Duration.ofDays(7);

  private final WorkspaceRepository workspaces;
  private final WorkspaceMemberRepository members;
  private final UserRepository users;
  private final WorkspaceInvitationRepository invitations;
  private final InvitationTokenService invitationTokens;
  private final CurrentUser currentUser;
  private final MembershipGuard guard;
  private final ActivityService activity;
  private final ApiMapper mapper;

  public WorkspaceService(
      WorkspaceRepository workspaces,
      WorkspaceMemberRepository members,
      UserRepository users,
      WorkspaceInvitationRepository invitations,
      InvitationTokenService invitationTokens,
      CurrentUser currentUser,
      MembershipGuard guard,
      ActivityService activity,
      ApiMapper mapper) {
    this.workspaces = workspaces;
    this.members = members;
    this.users = users;
    this.invitations = invitations;
    this.invitationTokens = invitationTokens;
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
    Workspace workspace =
        workspaces
            .findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));
    return addMemberToWorkspace(workspace, user, request.role(), actor);
  }

  /**
   * Adds an existing account immediately, or creates/renews a pending invitation for an email that
   * has not registered yet. The holder claims it through its secure registration link.
   */
  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public InviteOrAddResponse inviteOrAdd(UUID workspaceId, MemberRequest request) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN);
    Workspace workspace =
        workspaces
            .findById(workspaceId)
            .orElseThrow(() -> new NotFoundException("Workspace not found"));
    String email = request.email().trim().toLowerCase(Locale.ROOT);

    Optional<User> existingUser = users.findByEmailIgnoreCase(email);
    if (existingUser.isPresent()) {
      return new InviteOrAddResponse(
          "ADDED",
          addMemberToWorkspace(workspace, existingUser.get(), request.role(), actor),
          null,
          null);
    }

    Instant expiresAt = Instant.now().plus(INVITATION_VALIDITY);
    String rawToken = invitationTokens.generate();
    String tokenHash = invitationTokens.hash(rawToken);
    WorkspaceInvitation invitation =
        invitations
            .findByWorkspaceIdAndEmailIgnoreCase(workspaceId, email)
            .map(
                value -> {
                  value.renew(request.role(), actor, expiresAt, tokenHash);
                  return value;
                })
            .orElseGet(
                () ->
                    invitations.save(
                        new WorkspaceInvitation(
                            workspace, email, request.role(), actor, expiresAt, tokenHash)));
    activity.record(
        workspace,
        null,
        actor,
        EventType.MEMBER_INVITED,
        actor.getDisplayName()
            + " created a pending invitation for "
            + email
            + " as "
            + request.role());
    return new InviteOrAddResponse("INVITED", null, mapper.invitation(invitation), rawToken);
  }

  @Transactional(readOnly = true)
  public List<InvitationResponse> listInvitations(UUID workspaceId) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN);
    Instant now = Instant.now();
    return invitations.findAllWithInviterByWorkspaceId(workspaceId).stream()
        .filter(invitation -> !invitation.isExpired(now))
        .map(mapper::invitation)
        .toList();
  }

  @Transactional
  @Caching(
      evict = {
        @CacheEvict(cacheNames = "dashboard", allEntries = true),
        @CacheEvict(cacheNames = "tasks", allEntries = true),
        @CacheEvict(cacheNames = "projects", allEntries = true)
      })
  public void cancelInvitation(UUID workspaceId, UUID invitationId) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN);
    WorkspaceInvitation invitation =
        invitations
            .findById(invitationId)
            .orElseThrow(() -> new NotFoundException("Invitation not found"));
    if (!invitation.getWorkspace().getId().equals(workspaceId))
      throw new NotFoundException("Invitation not found");
    activity.record(
        invitation.getWorkspace(),
        null,
        actor,
        EventType.MEMBER_INVITATION_CANCELLED,
        actor.getDisplayName() + " cancelled the pending invitation for " + invitation.getEmail());
    invitations.delete(invitation);
  }

  @Transactional
  public InviteOrAddResponse regenerateInvitation(UUID workspaceId, UUID invitationId) {
    User actor = currentUser.require();
    guard.requireAny(workspaceId, actor.getId(), WorkspaceRole.ADMIN);
    WorkspaceInvitation invitation =
        invitations
            .findById(invitationId)
            .orElseThrow(() -> new NotFoundException("Invitation not found"));
    if (!invitation.getWorkspace().getId().equals(workspaceId))
      throw new NotFoundException("Invitation not found");
    String rawToken = invitationTokens.generate();
    invitation.renew(
        invitation.getRole(),
        actor,
        Instant.now().plus(INVITATION_VALIDITY),
        invitationTokens.hash(rawToken));
    activity.record(
        invitation.getWorkspace(),
        null,
        actor,
        EventType.MEMBER_INVITED,
        actor.getDisplayName()
            + " refreshed the pending invitation link for "
            + invitation.getEmail());
    return new InviteOrAddResponse("INVITED", null, mapper.invitation(invitation), rawToken);
  }

  @Transactional(readOnly = true)
  public InvitationPreviewResponse previewInvitation(String rawToken) {
    WorkspaceInvitation invitation = invitationByToken(rawToken);
    if (invitation.isExpired(Instant.now()))
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "This invitation link is invalid or has expired");
    return new InvitationPreviewResponse(
        invitation.getEmail(), invitation.getWorkspace().getName(), invitation.getExpiresAt());
  }

  private WorkspaceInvitation invitationByToken(String rawToken) {
    if (rawToken == null || rawToken.isBlank())
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "This invitation link is invalid or has expired");
    return invitations
        .findByTokenHash(invitationTokens.hash(rawToken))
        .orElseThrow(
            () ->
                new ApiException(
                    HttpStatus.BAD_REQUEST, "This invitation link is invalid or has expired"));
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
    WorkspaceRole previousRole = target.getRole();
    target.setRole(request.role());
    if (previousRole != request.role())
      activity.record(
          target.getWorkspace(),
          null,
          actor,
          EventType.MEMBER_ROLE_CHANGED,
          actor.getDisplayName()
              + " changed "
              + target.getUser().getDisplayName()
              + " from "
              + previousRole
              + " to "
              + request.role());
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
    activity.record(
        target.getWorkspace(),
        null,
        actor,
        EventType.MEMBER_REMOVED,
        actor.getDisplayName()
            + " removed "
            + target.getUser().getDisplayName()
            + " from the workspace");
    members.delete(target);
  }

  private MemberResponse addMemberToWorkspace(
      Workspace workspace, User user, WorkspaceRole role, User actor) {
    if (members.existsByWorkspaceIdAndUserId(workspace.getId(), user.getId()))
      throw new ConflictException("User is already a workspace member");
    WorkspaceMember membership = members.save(new WorkspaceMember(workspace, user, role));
    activity.record(
        workspace,
        null,
        actor,
        EventType.MEMBER_ADDED,
        actor.getDisplayName() + " added " + user.getDisplayName() + " as " + role);
    return mapper.member(membership);
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
