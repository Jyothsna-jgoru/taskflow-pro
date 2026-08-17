package com.taskflowpro.repository;

import com.taskflowpro.entity.WorkspaceMember;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
  Optional<WorkspaceMember> findByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

  List<WorkspaceMember> findAllByUserIdOrderByJoinedAtAsc(UUID userId);

  List<WorkspaceMember> findAllByWorkspaceIdOrderByJoinedAtAsc(UUID workspaceId);

  boolean existsByWorkspaceIdAndUserId(UUID workspaceId, UUID userId);

  long countByWorkspaceIdAndRole(UUID workspaceId, com.taskflowpro.entity.WorkspaceRole role);

  @Query(
      "select m from WorkspaceMember m join fetch m.workspace where m.user.id = :userId order by m.joinedAt")
  List<WorkspaceMember> findMembershipsWithWorkspace(@Param("userId") UUID userId);
}
