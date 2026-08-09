package com.systemdesign.bookhive.catalog.security;

import com.systemdesign.bookhive.catalog.common.JwtPayload;
import com.systemdesign.bookhive.catalog.shared.exception.UnauthorizedException;
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

/**
 * A stateless bearer-token check against the shared JWT_SECRET - no call back to auth-service
 * required. This is what lets catalog-service verify a caller's identity independently: it
 * could be scaled, redeployed, or briefly down without catalog-service losing the ability to
 * check who's calling it. catalog-service never issues tokens, only verifies them, so this
 * component (unlike auth-service's JwtService) has no signing method at all.
 */
@Component
public class JwtVerifier {

    private final SecretKey signingKey;

    public JwtVerifier(@Value("${jwt.secret}") String secret) {
        this.signingKey = Keys.hmacShaKeyFor(sha256(secret));
    }

    /** Extracts and verifies the bearer token from an {@code Authorization} header value. */
    public JwtPayload requireBearer(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new UnauthorizedException("Missing bearer token");
        }
        String token = authorizationHeader.substring(7);
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new JwtPayload(claims.getSubject(), claims.get("email", String.class));
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
