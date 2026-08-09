package com.systemdesign.blogstack.auth;

import com.systemdesign.blogstack.auth.dto.AuthResponse;
import com.systemdesign.blogstack.auth.dto.LoginRequest;
import com.systemdesign.blogstack.auth.dto.RegisterRequest;
import com.systemdesign.blogstack.auth.security.JwtService;
import com.systemdesign.blogstack.shared.exception.ConflictException;
import com.systemdesign.blogstack.shared.exception.UnauthorizedException;
import com.systemdesign.blogstack.users.UsersService;
import com.systemdesign.blogstack.users.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Plain monolith style: {@code AuthService} injects {@link UsersService} and calls its methods
 * directly, in-process, in the same request -- no HTTP call, no event, no interface contract
 * between them. Mirrors {@code src/modules/auth/auth.service.ts}.
 */
@Slf4j
@Service
public class AuthService {

    private final UsersService usersService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UsersService usersService, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.usersService = usersService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest dto) {
        usersService.findByEmail(dto.email()).ifPresent(existing -> {
            throw new ConflictException("An account with this email already exists");
        });

        String passwordHash = passwordEncoder.encode(dto.password());
        User user = usersService.create(dto.email(), passwordHash, dto.displayName());

        log.info("Registered new user {} ({})", user.getId(), user.getEmail());
        return issueToken(user);
    }

    public AuthResponse login(LoginRequest dto) {
        User user = usersService.findByEmail(dto.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return issueToken(user);
    }

    private AuthResponse issueToken(User user) {
        String accessToken = jwtService.generateToken(user);
        return new AuthResponse(
                accessToken,
                new AuthResponse.UserSummary(user.getId(), user.getEmail(), user.getDisplayName()));
    }
}
