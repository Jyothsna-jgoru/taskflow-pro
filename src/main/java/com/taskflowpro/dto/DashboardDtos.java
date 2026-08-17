package com.taskflowpro.dto;

import com.taskflowpro.entity.TaskStatus;
import java.util.*;

public final class DashboardDtos {
  private DashboardDtos() {}

  public record Workload(UUID userId, String displayName, long activeTasks) {}

  public record ProjectProgress(
      UUID projectId, String projectName, long totalTasks, long completedTasks, int percentage) {}

  public record DashboardResponse(
      long totalTasks,
      Map<TaskStatus, Long> tasksByStatus,
      long overdueTasks,
      long tasksDueThisWeek,
      int completionPercentage,
      List<ProjectProgress> projectProgress,
      List<Workload> workload,
      List<TaskDtos.ActivityResponse> recentActivity) {}
}
