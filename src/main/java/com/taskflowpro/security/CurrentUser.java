package com.taskflowpro.security;

import com.taskflowpro.entity.User;
import com.taskflowpro.exception.ApiException;
import com.taskflowpro.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
  private final UserRepository users;

  public CurrentUser(UserRepository users) {
    this.users = users;
  }

  public User require() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName()))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required");
    return users
        .findByEmailIgnoreCase(auth.getName())
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Authentication is required"));
  }
}
