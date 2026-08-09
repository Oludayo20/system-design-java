package com.systemdesign.blogstack.auth;

import com.systemdesign.blogstack.auth.dto.AuthResponse;
import com.systemdesign.blogstack.auth.dto.LoginRequest;
import com.systemdesign.blogstack.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "auth", description = "Registration, login, JWT issuance")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register a new BlogStack account",
            description = "Creates a user, hashes the password with bcrypt, and returns a JWT access token.")
    @ApiResponse(responseCode = "201", description = "Account created.", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "409", description = "An account with this email already exists.")
    public AuthResponse register(@Valid @RequestBody RegisterRequest dto) {
        return authService.register(dto);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Log in with email and password", description = "Validates credentials and returns a JWT access token.")
    @ApiResponse(responseCode = "200", description = "Login successful.", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    @ApiResponse(responseCode = "401", description = "Invalid email or password.")
    public AuthResponse login(@Valid @RequestBody LoginRequest dto) {
        return authService.login(dto);
    }
}
