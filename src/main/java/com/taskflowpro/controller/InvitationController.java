package com.taskflowpro.controller;

import com.taskflowpro.dto.WorkspaceDtos.InvitationPreviewResponse;
import com.taskflowpro.service.WorkspaceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/** Public lookup used only by a holder of an opaque invitation link. */
@RestController
@RequestMapping("/api/invitations")
@Tag(name = "Invitations")
public class InvitationController {
  private final WorkspaceService workspaces;

  public InvitationController(WorkspaceService workspaces) {
    this.workspaces = workspaces;
  }

  @GetMapping("/{token}")
  public InvitationPreviewResponse preview(@PathVariable String token) {
    return workspaces.previewInvitation(token);
  }
}
