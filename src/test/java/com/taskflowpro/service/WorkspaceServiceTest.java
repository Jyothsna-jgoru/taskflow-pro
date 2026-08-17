package com.taskflowpro.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.taskflowpro.dto.WorkspaceDtos.RoleRequest;
import com.taskflowpro.dto.WorkspaceDtos.WorkspaceRequest;
import com.taskflowpro.entity.*;
import com.taskflowpro.exception.ConflictException;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.*;
import com.taskflowpro.security.CurrentUser;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {
  @Mock WorkspaceRepository workspaces;
  @Mock WorkspaceMemberRepository members;
  @Mock UserRepository users;
  @Mock CurrentUser currentUser;
  @Mock MembershipGuard guard;
  @Mock ActivityService activity;

  WorkspaceService service;
  User actor;

  @BeforeEach
  void setUp() {
    service =
        new WorkspaceService(
            workspaces, members, users, currentUser, guard, activity, new ApiMapper());
    actor = new User("admin@example.com", "hash", "Admin User");
    when(currentUser.require()).thenReturn(actor);
  }

  @Test
  void creatorBecomesWorkspaceAdmin() {
    when(workspaces.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(members.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    var response = service.create(new WorkspaceRequest("Delivery", "Ship work together"));

    assertEquals(WorkspaceRole.ADMIN, response.currentUserRole());
    assertEquals(1, response.memberCount());
    verify(members).save(argThat(member -> member.getRole() == WorkspaceRole.ADMIN));
    verify(activity)
        .record(any(), isNull(), eq(actor), eq(EventType.WORKSPACE_CREATED), contains("created"));
  }

  @Test
  void refusesToDemoteTheOnlyAdmin() {
    Workspace workspace = new Workspace("Delivery", null, actor);
    WorkspaceMember onlyAdmin = new WorkspaceMember(workspace, actor, WorkspaceRole.ADMIN);
    when(members.findById(onlyAdmin.getId())).thenReturn(Optional.of(onlyAdmin));
    when(members.countByWorkspaceIdAndRole(workspace.getId(), WorkspaceRole.ADMIN)).thenReturn(1L);

    assertThrows(
        ConflictException.class,
        () ->
            service.updateRole(
                workspace.getId(), onlyAdmin.getId(), new RoleRequest(WorkspaceRole.MEMBER)));

    assertEquals(WorkspaceRole.ADMIN, onlyAdmin.getRole());
  }
}
