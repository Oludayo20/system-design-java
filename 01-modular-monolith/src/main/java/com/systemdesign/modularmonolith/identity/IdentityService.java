package com.systemdesign.modularmonolith.identity;

import com.systemdesign.modularmonolith.identity.dto.AuthResponse;
import com.systemdesign.modularmonolith.identity.dto.LoginRequest;
import com.systemdesign.modularmonolith.identity.dto.RegisterRequest;
import com.systemdesign.modularmonolith.identity.entity.User;
import com.systemdesign.modularmonolith.identity.repository.UserRepository;
import com.systemdesign.modularmonolith.identity.security.JwtService;
import com.systemdesign.modularmonolith.infrastructure.redis.RedisService;
import com.systemdesign.modularmonolith.shared.exception.ConflictException;
import com.systemdesign.modularmonolith.shared.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/** Mirrors {@code src/modules/identity/identity.service.ts}. */
@Slf4j
@Service
public class IdentityService {

    private static final long SESSION_TTL_SECONDS = 60L * 60 * 24 * 7; // 7 days

    private final UserRepository users;
    private final JwtService jwtService;
    private final RedisService redisService;
    private final PasswordEncoder passwordEncoder;

    public IdentityService(UserRepository users, JwtService jwtService, RedisService redisService,
                            PasswordEncoder passwordEncoder) {
        this.users = users;
        this.jwtService = jwtService;
        this.redisService = redisService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest dto) {
        users.findByEmail(dto.email()).ifPresent(existing -> {
            throw new ConflictException("An account with this email already exists");
        });

        User user = new User();
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setFullName(dto.fullName());
        user.setRoles(List.of(UserRole.CUSTOMER));
        user = users.save(user);

        log.info("Registered new user {} ({})", user.getId(), user.getEmail());
        return issueSession(user);
    }

    public AuthResponse login(LoginRequest dto) {
        User user = users.findByEmail(dto.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return issueSession(user);
    }

    private AuthResponse issueSession(User user) {
        String accessToken = jwtService.generateToken(user);

        // Session record in Redis: lets us support fast "who's logged in" lookups / future
        // token revocation without adding load to Postgres on every authenticated request.
        redisService.setJson("session:" + user.getId(), Map.of("email", user.getEmail()), SESSION_TTL_SECONDS);

        return new AuthResponse(
                accessToken,
                new AuthResponse.UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getRoles()));
    }
}
