package com.systemdesign.bookhive.auth.auth;

import com.systemdesign.bookhive.auth.auth.dto.AuthResponse;
import com.systemdesign.bookhive.auth.auth.dto.LoginRequest;
import com.systemdesign.bookhive.auth.auth.dto.RegisterRequest;
import com.systemdesign.bookhive.auth.auth.dto.VerifyResponse;
import com.systemdesign.bookhive.auth.shared.exception.UnauthorizedException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "auth")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new BookHive user and receive a JWT")
    @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    public AuthResponse register(@Valid @RequestBody RegisterRequest dto) {
        return authService.register(dto);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Log in with email and password")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
    public AuthResponse login(@Valid @RequestBody LoginRequest dto) {
        return authService.login(dto);
    }

    @GetMapping("/verify")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Verify a bearer token",
            description = "Decodes and verifies a JWT against JWT_SECRET and echoes the claims back. "
                    + "catalog-service and order-service do NOT call this endpoint on every request - "
                    + "they hold the same JWT_SECRET and verify tokens in-process. This endpoint exists "
                    + "so you can sanity-check a token by hand.")
    public VerifyResponse verify(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = (authorization != null && authorization.startsWith("Bearer "))
                ? authorization.substring(7)
                : null;
        if (token == null) {
            throw new UnauthorizedException("Missing bearer token");
        }
        return new VerifyResponse(true, authService.verify(token));
    }
}
