package com.taskflowpro.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecretKey key;
  private final Duration ttl;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.ttl-minutes:60}") long ttlMinutes) {
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.ttl = Duration.ofMinutes(ttlMinutes);
  }

  public String issue(String email) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(email)
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plus(ttl)))
        .signWith(key)
        .compact();
  }

  public String subject(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
  }

  public boolean valid(String token) {
    try {
      subject(token);
      return true;
    } catch (JwtException | IllegalArgumentException ignored) {
      return false;
    }
  }

  public long expiresInSeconds() {
    return ttl.toSeconds();
  }
}
