package com.taskflowpro.repository;

import com.taskflowpro.entity.WorkspaceInvitation;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, UUID> {
  Optional<WorkspaceInvitation> findByWorkspaceIdAndEmailIgnoreCase(UUID workspaceId, String email);

  Optional<WorkspaceInvitation> findByTokenHash(String tokenHash);

  List<WorkspaceInvitation> findAllByEmailIgnoreCase(String email);

  @Query(
      "select i from WorkspaceInvitation i join fetch i.invitedBy "
          + "where i.workspace.id = :workspaceId order by i.createdAt desc")
  List<WorkspaceInvitation> findAllWithInviterByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
