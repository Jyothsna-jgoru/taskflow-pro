package com.taskflowpro.controller;

import com.taskflowpro.dto.ProjectDtos.*;
import com.taskflowpro.service.ProjectService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/projects")
@Tag(name = "Projects")
public class ProjectController {
  private final ProjectService service;

  public ProjectController(ProjectService service) {
    this.service = service;
  }

  @GetMapping
  public List<ProjectResponse> list(@PathVariable UUID workspaceId) {
    return service.list(workspaceId);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProjectResponse create(
      @PathVariable UUID workspaceId, @Valid @RequestBody ProjectRequest request) {
    return service.create(workspaceId, request);
  }

  @GetMapping("/{projectId}")
  public ProjectResponse get(@PathVariable UUID workspaceId, @PathVariable UUID projectId) {
    return service.get(workspaceId, projectId);
  }

  @PutMapping("/{projectId}")
  public ProjectResponse update(
      @PathVariable UUID workspaceId,
      @PathVariable UUID projectId,
      @Valid @RequestBody ProjectRequest request) {
    return service.update(workspaceId, projectId, request);
  }

  @PostMapping("/{projectId}/archive")
  public ProjectResponse archive(@PathVariable UUID workspaceId, @PathVariable UUID projectId) {
    return service.archive(workspaceId, projectId);
  }
}
