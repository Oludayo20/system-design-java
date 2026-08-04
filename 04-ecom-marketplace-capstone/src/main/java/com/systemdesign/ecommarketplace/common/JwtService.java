package com.systemdesign.ecommarketplace.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Mirrors src/common/jwt-common.module.ts (JwtModule.registerAsync) plus the
 * sign/verify calls AuthService and JwtAuthGuard make against @nestjs/jwt's
 * JwtService.
 *
 * <p>Deviation: jjwt enforces a minimum 256-bit HMAC-SHA key, but the sample
 * JWT_SECRET in .env.example ("change_me_in_production_please", 30 bytes) is
 * shorter than that and Node's jsonwebtoken/@nestjs/jwt impose no such
 * minimum. To keep the sample .env working out of the box while still using
 * a spec-compliant key, the configured secret is stretched to exactly 256
 * bits via SHA-256 before being used as the signing key, rather than used
 * directly as raw key bytes.
 */
@Component
public class JwtService {

  private final SecretKey key;
  private final long expirationSeconds;

  public JwtService(
      @Value("${app.jwt.secret}") String secret, @Value("${app.jwt.expires-in}") String expiresIn) {
    this.key = deriveKey(secret);
    this.expirationSeconds = parseDurationSeconds(expiresIn);
  }

  public String issue(JwtPayload payload) {
    Instant now = Instant.now();
    return Jwts.builder()
        .subject(payload.sub())
        .claim("email", payload.email())
        .issuedAt(Date.from(now))
        .expiration(Date.from(now.plusSeconds(expirationSeconds)))
        .signWith(key)
        .compact();
  }

  /** @throws JwtException if the token is missing, malformed, expired, or has a bad signature. */
  public JwtPayload verify(String token) {
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    return new JwtPayload(claims.getSubject(), claims.get("email", String.class));
  }

  private static SecretKey deriveKey(String secret) {
    try {
      MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      byte[] keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
      return io.jsonwebtoken.security.Keys.hmacShaKeyFor(keyBytes);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  /**
   * Minimal parser for @nestjs/jwt-style expiresIn strings ("1h", "30m",
   * "45s", "2d", or a bare number of seconds).
   */
  private static long parseDurationSeconds(String expiresIn) {
    String value = expiresIn.trim().toLowerCase();
    if (value.isEmpty()) {
      return 3600;
    }
    char unit = value.charAt(value.length() - 1);
    if (Character.isDigit(unit)) {
      return Long.parseLong(value);
    }
    long amount = Long.parseLong(value.substring(0, value.length() - 1));
    return switch (unit) {
      case 's' -> amount;
      case 'm' -> amount * 60;
      case 'h' -> amount * 3600;
      case 'd' -> amount * 86400;
      default -> throw new IllegalArgumentException("Unsupported duration unit in: " + expiresIn);
    };
  }
}
