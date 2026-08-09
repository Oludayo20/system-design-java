package com.systemdesign.bookhive.auth.security;

import com.systemdesign.bookhive.auth.common.JwtPayload;
import com.systemdesign.bookhive.auth.user.User;
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

/**
 * Signs and verifies BookHive's JWTs. The only service that ISSUES tokens - catalog-service and
 * order-service only ever verify them, using the same shared {@code JWT_SECRET}.
 *
 * <p>The configured secret is hashed with SHA-256 before being used as the HMAC key: jjwt
 * enforces the JWA-mandated minimum 256-bit key size for HS256, and this project's default dev
 * secret ("change_me_in_production_please") is shorter than that. Hashing derives a fixed
 * 256-bit key deterministically from any secret length - every BookHive service does the exact
 * same derivation, so "same secret -> same key" holds across all three services that touch
 * JWTs.</p>
 */
@Component
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiresIn;

    public JwtService(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expires-in}") String expiresIn) {
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
    public JwtPayload parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new JwtPayload(claims.getSubject(), claims.get("email", String.class));
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
