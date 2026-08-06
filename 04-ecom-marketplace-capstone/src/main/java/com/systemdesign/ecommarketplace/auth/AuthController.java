package com.systemdesign.ecommarketplace.auth;

import com.systemdesign.ecommarketplace.auth.dto.AuthResponse;
import com.systemdesign.ecommarketplace.auth.dto.LoginRequest;
import com.systemdesign.ecommarketplace.auth.dto.RegisterRequest;
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

@Tag(name = "auth", description = "Register and login; writes shard + email directory")
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Register a new user", description = "Writes User+Wallet on shard, email→shard directory on primary, issues JWT with signup bonus.")
  @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
  @ApiResponse(responseCode = "409", description = "Email already registered.")
  public AuthResponse register(@Valid @RequestBody RegisterRequest dto) {
    return authService.register(dto);
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  @Operation(summary = "Log in", description = "Resolves shard via user_directory on primary, queries exactly one shard.")
  @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = AuthResponse.class)))
  @ApiResponse(responseCode = "401", description = "Invalid credentials.")
  public AuthResponse login(@Valid @RequestBody LoginRequest dto) {
    return authService.login(dto);
  }
}
