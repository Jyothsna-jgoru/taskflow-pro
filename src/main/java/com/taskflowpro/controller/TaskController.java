package com.taskflowpro.controller;

import com.taskflowpro.dto.TaskDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.service.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/tasks")
@Tag(name = "Tasks")
public class TaskController {
  private final TaskService tasks;
  private final TaskDetailService details;

  public TaskController(TaskService tasks, TaskDetailService details) {
    this.tasks = tasks;
    this.details = details;
  }

  @GetMapping
  public TaskPageResponse list(
      @PathVariable UUID workspaceId,
      @RequestParam(required = false) TaskStatus status,
      @RequestParam(required = false) TaskPriority priority,
      @RequestParam(required = false) UUID assigneeId,
      @RequestParam(required = false) UUID projectId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dueAfter,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate dueBefore,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "updatedAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return tasks.list(
        workspaceId,
        status,
        priority,
        assigneeId,
        projectId,
        dueAfter,
        dueBefore,
        search,
        page,
        size,
        sort,
        direction);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TaskResponse create(
      @PathVariable UUID workspaceId, @Valid @RequestBody TaskCreateRequest request) {
    return tasks.create(workspaceId, request);
  }

  @GetMapping("/{taskId}")
  public TaskDetailResponse get(@PathVariable UUID workspaceId, @PathVariable UUID taskId) {
    return details.get(workspaceId, taskId);
  }

  @PutMapping("/{taskId}")
  public TaskResponse update(
      @PathVariable UUID workspaceId,
      @PathVariable UUID taskId,
      @Valid @RequestBody TaskUpdateRequest request) {
    return tasks.update(workspaceId, taskId, request);
  }

  @DeleteMapping("/{taskId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable UUID workspaceId, @PathVariable UUID taskId) {
    tasks.delete(workspaceId, taskId);
  }
}
