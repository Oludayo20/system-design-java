package com.systemdesign.bookhive.auth.auth;

import com.systemdesign.bookhive.auth.auth.dto.AuthResponse;
import com.systemdesign.bookhive.auth.auth.dto.LoginRequest;
import com.systemdesign.bookhive.auth.auth.dto.RegisterRequest;
import com.systemdesign.bookhive.auth.common.JwtPayload;
import com.systemdesign.bookhive.auth.security.JwtService;
import com.systemdesign.bookhive.auth.shared.exception.ConflictException;
import com.systemdesign.bookhive.auth.shared.exception.UnauthorizedException;
import com.systemdesign.bookhive.auth.user.User;
import com.systemdesign.bookhive.auth.user.UserRepository;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, JwtService jwtService, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AuthResponse register(RegisterRequest dto) {
        users.findByEmail(dto.email()).ifPresent(existing -> {
            throw new ConflictException("Email already registered");
        });

        User user = new User();
        user.setEmail(dto.email());
        user.setPasswordHash(passwordEncoder.encode(dto.password()));
        user.setFullName(dto.fullName());
        user = users.save(user);

        log.info("Registered {} ({})", user.getEmail(), user.getId());
        return toResponse(user, jwtService.generateToken(user));
    }

    public AuthResponse login(LoginRequest dto) {
        User user = users.findByEmail(dto.email())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }

        return toResponse(user, jwtService.generateToken(user));
    }

    /** Verifies a bearer token and echoes the decoded claims - lets a client (or curl) sanity-check a token without decoding it by hand. */
    public JwtPayload verify(String token) {
        try {
            return jwtService.parseToken(token);
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid or expired token");
        }
    }

    private AuthResponse toResponse(User user, String accessToken) {
        return new AuthResponse(accessToken, new AuthResponse.UserSummary(user.getId(), user.getEmail(), user.getFullName()));
    }
}
