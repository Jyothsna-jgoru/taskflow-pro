package com.taskflowpro.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.taskflowpro.dto.AuthDtos.*;
import com.taskflowpro.security.JwtService;
import com.taskflowpro.service.AuthService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = AuthController.class,
    excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AuthControllerWebTest {
  @Autowired MockMvc mvc;
  @MockitoBean AuthService auth;
  @MockitoBean JwtService jwtService;

  @Test
  void rejectsInvalidRegistrationPayload() throws Exception {
    mvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"A\",\"email\":\"bad\",\"password\":\"short\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors.email").exists());
  }

  @Test
  void returnsAccessTokenForValidLogin() throws Exception {
    UserResponse user =
        new UserResponse(UUID.randomUUID(), "Avery", "avery@example.com", Instant.now());
    when(auth.login(any())).thenReturn(new AuthResponse("signed.jwt", "Bearer", 3600, user));
    mvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"avery@example.com\",\"password\":\"Password1!\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("signed.jwt"))
        .andExpect(jsonPath("$.user.email").value("avery@example.com"));
  }
}
