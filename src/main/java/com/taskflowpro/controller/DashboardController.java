package com.taskflowpro.controller;

import com.taskflowpro.dto.DashboardDtos.DashboardResponse;
import com.taskflowpro.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/dashboard")
@Tag(name = "Dashboard")
public class DashboardController {
  private final DashboardService service;

  public DashboardController(DashboardService service) {
    this.service = service;
  }

  @GetMapping
  public DashboardResponse get(@PathVariable UUID workspaceId) {
    return service.get(workspaceId);
  }
}
