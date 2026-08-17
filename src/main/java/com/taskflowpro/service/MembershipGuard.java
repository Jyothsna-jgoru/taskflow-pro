package com.taskflowpro.service;

import com.taskflowpro.entity.*;
import com.taskflowpro.exception.*;
import com.taskflowpro.repository.WorkspaceMemberRepository;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class MembershipGuard {
  private final WorkspaceMemberRepository members;

  public MembershipGuard(WorkspaceMemberRepository members) {
    this.members = members;
  }

  public WorkspaceMember require(UUID workspaceId, UUID userId) {
    return members
        .findByWorkspaceIdAndUserId(workspaceId, userId)
        .orElseThrow(() -> new ForbiddenException("You are not a member of this workspace"));
  }

  public WorkspaceMember requireAny(UUID workspaceId, UUID userId, WorkspaceRole... roles) {
    WorkspaceMember member = require(workspaceId, userId);
    if (Arrays.stream(roles).noneMatch(role -> role == member.getRole()))
      throw new ForbiddenException("Your workspace role does not allow this action");
    return member;
  }

  public void requireMemberUser(UUID workspaceId, UUID userId) {
    if (!members.existsByWorkspaceIdAndUserId(workspaceId, userId))
      throw new BadRequestException("Selected user is not a workspace member");
  }
}
