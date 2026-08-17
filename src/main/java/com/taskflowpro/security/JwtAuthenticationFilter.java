package com.taskflowpro.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null
        && header.startsWith("Bearer ")
        && SecurityContextHolder.getContext().getAuthentication() == null) {
      String token = header.substring(7);
      if (jwtService.valid(token)) {
        String email = jwtService.subject(token);
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(email, null, List.of()));
      }
    }
    chain.doFilter(request, response);
  }
}
