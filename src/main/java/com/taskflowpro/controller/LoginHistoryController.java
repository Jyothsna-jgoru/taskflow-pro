package com.taskflowpro.controller;

import com.taskflowpro.dto.TaskDtos.ActivityResponse;
import com.taskflowpro.service.LoginHistoryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspaces/{workspaceId}/security")
@Tag(name = "Security")
public class LoginHistoryController {
  private final LoginHistoryService history;

  public LoginHistoryController(LoginHistoryService history) {
    this.history = history;
  }

  @GetMapping("/login-history")
  public List<ActivityResponse> loginHistory(
      @PathVariable UUID workspaceId, @RequestParam(defaultValue = "20") int limit) {
    return history.list(workspaceId, Math.max(1, Math.min(limit, 100)));
  }
}
