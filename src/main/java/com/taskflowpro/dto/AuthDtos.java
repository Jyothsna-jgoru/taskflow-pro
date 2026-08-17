package com.taskflowpro.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class AuthDtos {
  private AuthDtos() {}

  public record RegisterRequest(
      @NotBlank @Size(max = 100) String displayName,
      @NotBlank @Email @Size(max = 254) String email,
      @NotBlank @Size(min = 8, max = 72) String password) {}

  public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

  public record UserResponse(UUID id, String displayName, String email, Instant createdAt) {}

  public record AuthResponse(
      String accessToken, String tokenType, long expiresInSeconds, UserResponse user) {}
}
