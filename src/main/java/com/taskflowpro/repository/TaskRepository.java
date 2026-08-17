package com.taskflowpro.repository;

import com.taskflowpro.entity.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface TaskRepository
    extends JpaRepository<TaskItem, UUID>, JpaSpecificationExecutor<TaskItem> {
  long countByWorkspaceId(UUID workspaceId);

  long countByWorkspaceIdAndStatus(UUID workspaceId, TaskStatus status);

  long countByWorkspaceIdAndDueDateBeforeAndStatusNot(
      UUID workspaceId, LocalDate date, TaskStatus status);

  long countByWorkspaceIdAndDueDateBetweenAndStatusNot(
      UUID workspaceId, LocalDate start, LocalDate end, TaskStatus status);

  long countByProjectId(UUID projectId);

  long countByProjectIdAndStatus(UUID projectId, TaskStatus status);

  @Query(
      "select t.assignee.id, t.assignee.displayName, count(t) from TaskItem t where t.workspace.id = :workspaceId and t.assignee is not null and t.status <> com.taskflowpro.entity.TaskStatus.DONE group by t.assignee.id, t.assignee.displayName order by count(t) desc")
  List<Object[]> activeWorkload(UUID workspaceId);
}
