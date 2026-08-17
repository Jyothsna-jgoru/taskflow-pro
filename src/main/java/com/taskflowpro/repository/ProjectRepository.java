package com.taskflowpro.repository;

import com.taskflowpro.entity.*;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
  List<Project> findAllByWorkspaceIdOrderByCreatedAtDesc(UUID workspaceId);

  long countByWorkspaceIdAndStatus(UUID workspaceId, ProjectStatus status);
}
