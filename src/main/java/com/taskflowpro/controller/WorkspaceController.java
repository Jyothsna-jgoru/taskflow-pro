package com.taskflowpro.controller;

import com.taskflowpro.dto.WorkspaceDtos.*;
import com.taskflowpro.service.WorkspaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces")
@Tag(name = "Workspaces")
public class WorkspaceController {
  private final WorkspaceService service;

  public WorkspaceController(WorkspaceService service) {
    this.service = service;
  }

  @GetMapping
  public List<WorkspaceResponse> list() {
    return service.list();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public WorkspaceResponse create(@Valid @RequestBody WorkspaceRequest request) {
    return service.create(request);
  }

  @GetMapping("/{workspaceId}")
  public WorkspaceResponse get(@PathVariable UUID workspaceId) {
    return service.get(workspaceId);
  }

  @PutMapping("/{workspaceId}")
  public WorkspaceResponse update(
      @PathVariable UUID workspaceId, @Valid @RequestBody WorkspaceRequest request) {
    return service.update(workspaceId, request);
  }

  @GetMapping("/{workspaceId}/members")
  public List<MemberResponse> members(@PathVariable UUID workspaceId) {
    return service.listMembers(workspaceId);
  }

  @PostMapping("/{workspaceId}/members")
  @ResponseStatus(HttpStatus.CREATED)
  public MemberResponse addMember(
      @PathVariable UUID workspaceId, @Valid @RequestBody MemberRequest request) {
    return service.addMember(workspaceId, request);
  }

  @PostMapping("/{workspaceId}/members/invite-or-add")
  @ResponseStatus(HttpStatus.CREATED)
  public InviteOrAddResponse inviteOrAdd(
      @PathVariable UUID workspaceId, @Valid @RequestBody MemberRequest request) {
    return service.inviteOrAdd(workspaceId, request);
  }

  @GetMapping("/{workspaceId}/members/invitations")
  public List<InvitationResponse> invitations(@PathVariable UUID workspaceId) {
    return service.listInvitations(workspaceId);
  }

  @DeleteMapping("/{workspaceId}/members/invitations/{invitationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void cancelInvitation(@PathVariable UUID workspaceId, @PathVariable UUID invitationId) {
    service.cancelInvitation(workspaceId, invitationId);
  }

  @PostMapping("/{workspaceId}/members/invitations/{invitationId}/regenerate")
  public InviteOrAddResponse regenerateInvitation(
      @PathVariable UUID workspaceId, @PathVariable UUID invitationId) {
    return service.regenerateInvitation(workspaceId, invitationId);
  }

  @PatchMapping("/{workspaceId}/members/{membershipId}")
  public MemberResponse role(
      @PathVariable UUID workspaceId,
      @PathVariable UUID membershipId,
      @Valid @RequestBody RoleRequest request) {
    return service.updateRole(workspaceId, membershipId, request);
  }

  @DeleteMapping("/{workspaceId}/members/{membershipId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void remove(@PathVariable UUID workspaceId, @PathVariable UUID membershipId) {
    service.removeMember(workspaceId, membershipId);
  }
}
