package com.taskflowpro.service;

import com.taskflowpro.dto.TaskDtos.ActivityResponse;
import com.taskflowpro.entity.EventType;
import com.taskflowpro.entity.User;
import com.taskflowpro.entity.WorkspaceRole;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.ActivityEventRepository;
import com.taskflowpro.security.CurrentUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginHistoryService {
  private final ActivityEventRepository events;
  private final CurrentUser currentUser;
  private final MembershipGuard guard;
  private final ApiMapper mapper;

  public LoginHistoryService(
      ActivityEventRepository events,
      CurrentUser currentUser,
      MembershipGuard guard,
      ApiMapper mapper) {
    this.events = events;
    this.currentUser = currentUser;
    this.guard = guard;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  public List<ActivityResponse> list(UUID workspaceId, int limit) {
    User user = currentUser.require();
    guard.requireAny(workspaceId, user.getId(), WorkspaceRole.ADMIN);
    return events
        .findAllByWorkspaceIdAndEventTypeOrderByCreatedAtDesc(
            workspaceId, EventType.USER_SIGNED_IN, PageRequest.of(0, limit))
        .stream()
        .map(mapper::activity)
        .toList();
  }
}
