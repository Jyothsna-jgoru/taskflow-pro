package com.taskflowpro.service;

import com.taskflowpro.dto.AuthDtos.*;
import com.taskflowpro.entity.User;
import com.taskflowpro.exception.ApiException;
import com.taskflowpro.exception.ConflictException;
import com.taskflowpro.mapper.ApiMapper;
import com.taskflowpro.repository.UserRepository;
import com.taskflowpro.security.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserRepository users;
  private final PasswordEncoder passwords;
  private final JwtService jwt;
  private final CurrentUser currentUser;
  private final ApiMapper mapper;

  public AuthService(
      UserRepository users,
      PasswordEncoder passwords,
      JwtService jwt,
      CurrentUser currentUser,
      ApiMapper mapper) {
    this.users = users;
    this.passwords = passwords;
    this.jwt = jwt;
    this.currentUser = currentUser;
    this.mapper = mapper;
  }

  @Transactional
  public AuthResponse register(RegisterRequest request) {
    if (users.existsByEmailIgnoreCase(request.email()))
      throw new ConflictException("An account with this email already exists");
    User user =
        users.save(
            new User(request.email(), passwords.encode(request.password()), request.displayName()));
    return response(user);
  }

  @Transactional(readOnly = true)
  public AuthResponse login(LoginRequest request) {
    User user =
        users
            .findByEmailIgnoreCase(request.email())
            .orElseThrow(
                () -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
    if (!passwords.matches(request.password(), user.getPasswordHash()))
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    return response(user);
  }

  @Transactional(readOnly = true)
  public UserResponse me() {
    return mapper.user(currentUser.require());
  }

  private AuthResponse response(User user) {
    return new AuthResponse(
        jwt.issue(user.getEmail()), "Bearer", jwt.expiresInSeconds(), mapper.user(user));
  }
}
