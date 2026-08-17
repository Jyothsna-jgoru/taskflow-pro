package com.taskflowpro.controller;

import com.taskflowpro.dto.TaskDtos.*;
import com.taskflowpro.service.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tasks/{taskId}/comments")
@Tag(name = "Comments")
public class CommentController {
  private final CommentService service;

  public CommentController(CommentService service) {
    this.service = service;
  }

  @GetMapping
  public List<CommentResponse> list(@PathVariable UUID workspaceId, @PathVariable UUID taskId) {
    return service.list(workspaceId, taskId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CommentResponse add(
      @PathVariable UUID workspaceId,
      @PathVariable UUID taskId,
      @Valid @RequestBody CommentRequest request) {
    return service.add(workspaceId, taskId, request);
  }
}
