package com.systemdesign.blogstack.auth.security;

import com.systemdesign.blogstack.auth.AuthenticatedUser;
import com.systemdesign.blogstack.users.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Signs and verifies the app's JWTs. Mirrors {@code jwt.strategy.ts} (token verification ->
 * {@link AuthenticatedUser}) and the token-issuing half of {@code auth.service.ts#issueToken}
 * ({@code JwtService.signAsync}).
 *
 * <p>The configured secret is hashed with SHA-256 before being used as the HMAC key: jjwt enforces
 * the JWA-mandated minimum 256-bit key size for HS256, but Node's {@code jsonwebtoken} (used by
 * {@code @nestjs/jwt}) does not, and this project's default dev secret
 * ("change_me_in_production_please") is shorter than 256 bits. Hashing derives a fixed 256-bit
 * key deterministically from any secret length, preserving "same secret -> same key" behavior
 * while satisfying jjwt.</p>
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiresIn;

    public JwtService(@Value("${auth.jwt.secret}") String secret,
                       @Value("${auth.jwt.expires-in}") String expiresIn) {
        this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
        this.expiresIn = DurationParser.parse(expiresIn);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiresIn)))
                .signWith(signingKey)
                .compact();
    }

    /** @throws JwtException if the token is malformed, expired, or fails signature verification. */
    public AuthenticatedUser parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);

        return new AuthenticatedUser(userId, email);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
