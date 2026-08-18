package com.taskflowpro.repository;

import com.taskflowpro.entity.ActivityEvent;
import com.taskflowpro.entity.EventType;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {
  List<ActivityEvent> findAllByTaskIdOrderByCreatedAtDesc(UUID taskId);

  List<ActivityEvent> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId, Pageable pageable);

  List<ActivityEvent> findAllByWorkspaceIdAndEventTypeOrderByCreatedAtDesc(
      UUID workspaceId, EventType eventType, Pageable pageable);
}
