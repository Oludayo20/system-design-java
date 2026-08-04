package com.systemdesign.modularmonolith.identity.security;

import com.systemdesign.modularmonolith.identity.AuthenticatedUser;
import com.systemdesign.modularmonolith.identity.UserRole;
import com.systemdesign.modularmonolith.identity.entity.User;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Signs and verifies the app's JWTs. Mirrors {@code identity.strategies.jwt.strategy.ts} (token
 * verification -> {@link AuthenticatedUser}) and the token-issuing half of
 * {@code identity.service.ts#issueSession} ({@code JwtService.signAsync}).
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

    public JwtService(@Value("${identity.jwt.secret}") String secret,
                       @Value("${identity.jwt.expires-in}") String expiresIn) {
        this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
        this.expiresIn = DurationParser.parse(expiresIn);
    }

    public String generateToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream().map(UserRole::getValue).toList();

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("roles", roles)
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
        @SuppressWarnings("unchecked")
        List<String> rawRoles = claims.get("roles", List.class);
        List<UserRole> roles = rawRoles == null
                ? List.of(UserRole.CUSTOMER)
                : rawRoles.stream().map(UserRole::fromValue).collect(Collectors.toList());

        return new AuthenticatedUser(userId, email, roles);
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
