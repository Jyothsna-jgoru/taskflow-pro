package com.taskflowpro.service;

import com.taskflowpro.entity.*;
import com.taskflowpro.repository.ActivityEventRepository;
import org.springframework.stereotype.Service;

@Service
public class ActivityService {
  private final ActivityEventRepository events;

  public ActivityService(ActivityEventRepository events) {
    this.events = events;
  }

  public void record(
      Workspace workspace, TaskItem task, User actor, EventType type, String message) {
    events.save(new ActivityEvent(workspace, task, actor, type, message, null));
  }
}
