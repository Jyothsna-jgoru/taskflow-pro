package com.taskflowpro.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflowpro.exception.ApiError;
import com.taskflowpro.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
      throws Exception {
    return configuration.getAuthenticationManager();
  }

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationFilter jwt, ObjectMapper mapper) throws Exception {
    return http.csrf(csrf -> csrf.disable())
        .cors(cors -> {})
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/auth/register",
                        "/api/auth/login",
                        "/actuator/health/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (request, response, ex) -> {
                          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          mapper.writeValue(
                              response.getOutputStream(),
                              new ApiError(
                                  Instant.now(),
                                  401,
                                  "UNAUTHORIZED",
                                  "Authentication is required",
                                  request.getRequestURI(),
                                  null));
                        })
                    .accessDeniedHandler(
                        (request, response, ex) -> {
                          response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                          response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                          mapper.writeValue(
                              response.getOutputStream(),
                              new ApiError(
                                  Instant.now(),
                                  403,
                                  "FORBIDDEN",
                                  "Access is denied",
                                  request.getRequestURI(),
                                  null));
                        }))
        .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class)
        .build();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(@Value("${app.cors-origins}") String origins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Arrays.stream(origins.split(",")).map(String::trim).toList());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
    config.setExposedHeaders(List.of("Location"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
