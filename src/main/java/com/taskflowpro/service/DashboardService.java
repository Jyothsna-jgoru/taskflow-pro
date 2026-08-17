package com.taskflowpro.service;

import com.taskflowpro.dto.DashboardDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.*;
import com.taskflowpro.security.CurrentUser;
import java.time.LocalDate;
import java.util.*;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
  private final TaskRepository tasks;
  private final ProjectRepository projects;
  private final ActivityEventRepository events;
  private final WorkspaceMemberRepository members;
  private final CurrentUser currentUser;
  private final MembershipGuard guard;
  private final ApiMapper mapper;

  public DashboardService(
      TaskRepository tasks,
      ProjectRepository projects,
      ActivityEventRepository events,
      WorkspaceMemberRepository members,
      CurrentUser currentUser,
      MembershipGuard guard,
      ApiMapper mapper) {
    this.tasks = tasks;
    this.projects = projects;
    this.events = events;
    this.members = members;
    this.currentUser = currentUser;
    this.guard = guard;
    this.mapper = mapper;
  }

  @Transactional(readOnly = true)
  @Cacheable(
      cacheNames = "dashboard",
      key =
          "T(org.springframework.security.core.context.SecurityContextHolder).context.authentication.name + ':' + #workspaceId")
  public DashboardResponse get(UUID workspaceId) {
    User user = currentUser.require();
    guard.require(workspaceId, user.getId());
    long total = tasks.countByWorkspaceId(workspaceId);
    EnumMap<TaskStatus, Long> byStatus = new EnumMap<>(TaskStatus.class);
    for (TaskStatus status : TaskStatus.values())
      byStatus.put(status, tasks.countByWorkspaceIdAndStatus(workspaceId, status));
    long done = byStatus.get(TaskStatus.DONE);
    LocalDate today = LocalDate.now();
    long overdue =
        tasks.countByWorkspaceIdAndDueDateBeforeAndStatusNot(workspaceId, today, TaskStatus.DONE);
    long dueWeek =
        tasks.countByWorkspaceIdAndDueDateBetweenAndStatusNot(
            workspaceId, today, today.plusDays(7), TaskStatus.DONE);
    List<ProjectProgress> progress =
        projects.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId).stream()
            .filter(p -> p.getStatus() != ProjectStatus.ARCHIVED)
            .map(
                p -> {
                  long pt = tasks.countByProjectId(p.getId()),
                      pd = tasks.countByProjectIdAndStatus(p.getId(), TaskStatus.DONE);
                  return new ProjectProgress(
                      p.getId(),
                      p.getName(),
                      pt,
                      pd,
                      pt == 0 ? 0 : (int) Math.round(pd * 100.0 / pt));
                })
            .toList();
    Map<UUID, Long> activeByUser = new HashMap<>();
    tasks
        .activeWorkload(workspaceId)
        .forEach(row -> activeByUser.put((UUID) row[0], (Long) row[2]));
    List<Workload> workload =
        members.findAllByWorkspaceIdOrderByJoinedAtAsc(workspaceId).stream()
            .map(
                member ->
                    new Workload(
                        member.getUser().getId(),
                        member.getUser().getDisplayName(),
                        activeByUser.getOrDefault(member.getUser().getId(), 0L)))
            .sorted(Comparator.comparingLong(Workload::activeTasks).reversed())
            .toList();
    return new DashboardResponse(
        total,
        byStatus,
        overdue,
        dueWeek,
        total == 0 ? 0 : (int) Math.round(done * 100.0 / total),
        progress,
        workload,
        events.findAllByWorkspaceIdOrderByCreatedAtDesc(workspaceId, PageRequest.of(0, 12)).stream()
            .map(mapper::activity)
            .toList());
  }
}
