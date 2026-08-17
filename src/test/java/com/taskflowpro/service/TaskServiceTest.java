package com.taskflowpro.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.taskflowpro.dto.TaskDtos.*;
import com.taskflowpro.entity.*;
import com.taskflowpro.exception.*;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.*;
import com.taskflowpro.security.CurrentUser;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {
  @Mock TaskRepository tasks;
  @Mock ProjectService projects;
  @Mock UserRepository users;
  @Mock CurrentUser currentUser;
  @Mock MembershipGuard guard;
  @Mock ActivityService activity;
  TaskService service;
  User actor;
  Workspace workspace;
  Project project;
  TaskItem task;

  @BeforeEach
  void setUp() {
    service =
        new TaskService(tasks, projects, users, currentUser, guard, activity, new ApiMapper());
    actor = new User("owner@example.com", "hash", "Owner");
    workspace = new Workspace("Workspace", null, actor);
    project = new Project(workspace, "Project", null, ProjectStatus.ACTIVE, actor, null, null);
    task =
        new TaskItem(
            workspace,
            project,
            "Task",
            "Description",
            TaskStatus.TODO,
            TaskPriority.MEDIUM,
            actor,
            actor,
            LocalDate.now(),
            Set.of("api"));
  }

  @Test
  void rejectsStaleTaskUpdateBeforeWriting() {
    when(currentUser.require()).thenReturn(actor);
    when(tasks.findById(any())).thenReturn(Optional.of(task));
    TaskUpdateRequest request =
        new TaskUpdateRequest(
            project.getId(),
            "Changed",
            "Description",
            TaskStatus.DONE,
            TaskPriority.HIGH,
            actor.getId(),
            null,
            Set.of(),
            5L);
    assertThrows(
        ConflictException.class,
        () -> service.update(workspace.getId(), UUID.randomUUID(), request));
    verify(tasks, never()).saveAndFlush(any());
  }

  @Test
  void enforcesWorkspaceBoundaryBeforeListing() {
    when(currentUser.require()).thenReturn(actor);
    doThrow(new ForbiddenException("not a member")).when(guard).require(any(), eq(actor.getId()));
    assertThrows(
        ForbiddenException.class,
        () ->
            service.list(
                UUID.randomUUID(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                20,
                "updatedAt",
                "desc"));
    verifyNoInteractions(tasks);
  }
}
