package com.taskflowpro.config;

import com.taskflowpro.entity.*;
import com.taskflowpro.repository.*;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@ConditionalOnProperty(name = "taskflow.seed-demo-data", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {
  private final UserRepository users;
  private final WorkspaceRepository workspaces;
  private final WorkspaceMemberRepository members;
  private final ProjectRepository projects;
  private final TaskRepository tasks;
  private final CommentRepository comments;
  private final ActivityEventRepository events;
  private final PasswordEncoder passwords;

  public DemoDataSeeder(
      UserRepository users,
      WorkspaceRepository workspaces,
      WorkspaceMemberRepository members,
      ProjectRepository projects,
      TaskRepository tasks,
      CommentRepository comments,
      ActivityEventRepository events,
      PasswordEncoder passwords) {
    this.users = users;
    this.workspaces = workspaces;
    this.members = members;
    this.projects = projects;
    this.tasks = tasks;
    this.comments = comments;
    this.events = events;
    this.passwords = passwords;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (users.count() > 0) return;
    User admin =
        users.save(new User("admin@taskflow.local", passwords.encode("Admin123!"), "Avery Admin"));
    User manager =
        users.save(
            new User("manager@taskflow.local", passwords.encode("Manager123!"), "Morgan Lee"));
    User member =
        users.save(new User("member@taskflow.local", passwords.encode("Member123!"), "Maya Chen"));
    Workspace workspace =
        workspaces.save(
            new Workspace(
                "Northstar Product",
                "A demo workspace for shipping the next product milestone.",
                admin));
    members.save(new WorkspaceMember(workspace, admin, WorkspaceRole.ADMIN));
    members.save(new WorkspaceMember(workspace, manager, WorkspaceRole.MANAGER));
    members.save(new WorkspaceMember(workspace, member, WorkspaceRole.MEMBER));
    Project launch =
        projects.save(
            new Project(
                workspace,
                "Platform launch",
                "Prepare TaskFlow Pro for a public beta.",
                ProjectStatus.ACTIVE,
                manager,
                LocalDate.now().minusDays(18),
                LocalDate.now().plusDays(21)));
    Project design =
        projects.save(
            new Project(
                workspace,
                "Design system",
                "Unify interaction patterns and visual language.",
                ProjectStatus.ACTIVE,
                member,
                LocalDate.now().minusDays(8),
                LocalDate.now().plusDays(14)));
    Project research =
        projects.save(
            new Project(
                workspace,
                "Customer research",
                "Validate the first-run experience.",
                ProjectStatus.PLANNING,
                admin,
                LocalDate.now().plusDays(2),
                LocalDate.now().plusDays(30)));
    TaskItem t1 =
        tasks.save(
            new TaskItem(
                workspace,
                launch,
                "Finalize API error contract",
                "Review error codes and publish examples for frontend consumers.",
                TaskStatus.IN_REVIEW,
                TaskPriority.HIGH,
                manager,
                admin,
                LocalDate.now().plusDays(2),
                Set.of("backend", "api")));
    TaskItem t2 =
        tasks.save(
            new TaskItem(
                workspace,
                launch,
                "Harden workspace authorization",
                "Verify every workspace-scoped repository lookup is guarded.",
                TaskStatus.IN_PROGRESS,
                TaskPriority.URGENT,
                admin,
                manager,
                LocalDate.now().plusDays(1),
                Set.of("security", "backend")));
    TaskItem t3 =
        tasks.save(
            new TaskItem(
                workspace,
                launch,
                "Draft beta launch checklist",
                "Capture product, support, and engineering launch gates.",
                TaskStatus.TODO,
                TaskPriority.MEDIUM,
                member,
                manager,
                LocalDate.now().plusDays(5),
                Set.of("launch")));
    TaskItem t4 =
        tasks.save(
            new TaskItem(
                workspace,
                design,
                "Build accessible status badges",
                "Check contrast and non-color status cues.",
                TaskStatus.DONE,
                TaskPriority.MEDIUM,
                member,
                member,
                LocalDate.now().minusDays(1),
                Set.of("frontend", "a11y")));
    TaskItem t5 =
        tasks.save(
            new TaskItem(
                workspace,
                design,
                "Document empty states",
                "Write concise guidance for empty projects and boards.",
                TaskStatus.BLOCKED,
                TaskPriority.LOW,
                member,
                manager,
                LocalDate.now().minusDays(2),
                Set.of("content", "design")));
    TaskItem t6 =
        tasks.save(
            new TaskItem(
                workspace,
                research,
                "Prepare interview script",
                "Focus on task handoff and progress visibility.",
                TaskStatus.TODO,
                TaskPriority.HIGH,
                manager,
                admin,
                LocalDate.now().plusDays(8),
                Set.of("research")));
    comments.save(
        new Comment(
            t1,
            member,
            "The frontend now handles field-level validation errors. The contract looks consistent."));
    comments.save(
        new Comment(t2, manager, "Please include a negative test for cross-workspace task IDs."));
    events.save(
        new ActivityEvent(
            workspace,
            null,
            admin,
            EventType.WORKSPACE_CREATED,
            "Avery Admin created the workspace",
            null));
    events.save(
        new ActivityEvent(
            workspace,
            null,
            manager,
            EventType.PROJECT_CREATED,
            "Morgan Lee created project “Platform launch”",
            null));
    events.save(
        new ActivityEvent(
            workspace, t1, admin, EventType.TASK_CREATED, "Avery Admin created this task", null));
    events.save(
        new ActivityEvent(
            workspace,
            t1,
            manager,
            EventType.TASK_STATUS_CHANGED,
            "Morgan Lee moved the task to in review",
            null));
    events.save(
        new ActivityEvent(
            workspace, t2, manager, EventType.COMMENT_ADDED, "Morgan Lee added a comment", null));
  }
}
