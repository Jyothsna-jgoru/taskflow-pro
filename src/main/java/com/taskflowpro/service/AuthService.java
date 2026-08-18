package com.taskflowpro.service;

import com.taskflowpro.dto.AuthDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.exception.ApiException;
import com.taskflowpro.exception.ConflictException;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.UserRepository;
import com.taskflowpro.repository.WorkspaceMemberRepository;
import com.taskflowpro.repository.WorkspaceInvitationRepository;
import com.taskflowpro.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  private final CurrentUser currentUser;
  private final ApiMapper mapper;
  private final WorkspaceMemberRepository members;
  private final WorkspaceInvitationRepository invitations;
  private final InvitationTokenService invitationTokens;
  private final ActivityService activity;

  public AuthService(
      UserRepository users,
      PasswordEncoder passwords,
      JwtService jwt,
      CurrentUser currentUser,
      ApiMapper mapper,
      WorkspaceMemberRepository members,
      WorkspaceInvitationRepository invitations,
      InvitationTokenService invitationTokens,
      ActivityService activity) {
    this.users = users;
    this.passwords = passwords;
    this.jwt = jwt;
    this.currentUser = currentUser;
    this.mapper = mapper;
    this.members = members;
    this.invitations = invitations;
    this.invitationTokens = invitationTokens;
    this.activity = activity;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (users.existsByEmailIgnoreCase(request.email()))
      throw new ConflictException("An account with this email already exists");
    WorkspaceInvitation invitation = invitationForRegistration(request);
    User user =
        users.save(
            new User(request.email(), passwords.encode(request.password()), request.displayName()));
    claimInvitation(user, invitation);
    return response(user);
  }

  @Transactional
  public AuthResponse login(LoginRequest request) {
    User user =
        users
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(
                () -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
    if (!passwords.matches(request.password(), user.getPasswordHash()))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    members
        .findMembershipsWithWorkspace(user.getId())
        .forEach(
            membership ->
                activity.record(
                    membership.getWorkspace(),
                    null,
                    user,
                    EventType.USER_SIGNED_IN,
                    user.getDisplayName() + " signed in"));
    return response(user);
  }

  @Transactional(readOnly = true)
  public UserResponse me() {
    return mapper.user(currentUser.require());
  }

  private AuthResponse response(User user) {
    return new AuthResponse(
        jwt.issue(user.getEmail()), "Bearer", jwt.expiresInSeconds(), mapper.user(user));
  }

  private WorkspaceInvitation invitationForRegistration(RegisterRequest request) {
    if (request.invitationToken() == null || request.invitationToken().isBlank()) return null;
    WorkspaceInvitation invitation =
        invitations
            .findByTokenHash(invitationTokens.hash(request.invitationToken()))
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.BAD_REQUEST, "This invitation link is invalid or has expired"));
    if (invitation.isExpired(java.time.Instant.now())) {
      invitations.delete(invitation);
      throw new ApiException(HttpStatus.BAD_REQUEST, "This invitation link is invalid or has expired");
    }
    if (!invitation.getEmail().equalsIgnoreCase(request.email().trim()))
      throw new ApiException(
          HttpStatus.BAD_REQUEST, "Register using the email address assigned to this invitation");
    return invitation;
  }

  private void claimInvitation(User user, WorkspaceInvitation invitation) {
    if (invitation == null) return;
    members.save(new WorkspaceMember(invitation.getWorkspace(), user, invitation.getRole()));
    activity.record(
        invitation.getWorkspace(),
        null,
        user,
        EventType.MEMBER_JOINED,
        user.getDisplayName() + " joined using a pending invitation");
    invitations.delete(invitation);
  }
}
