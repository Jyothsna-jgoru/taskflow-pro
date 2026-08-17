package com.taskflowpro.repository;

import com.taskflowpro.entity.Comment;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
  List<Comment> findAllByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
