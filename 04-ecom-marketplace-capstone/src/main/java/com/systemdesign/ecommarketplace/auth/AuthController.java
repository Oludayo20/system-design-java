package com.systemdesign.ecommarketplace.auth;

import com.systemdesign.ecommarketplace.auth.dto.AuthResponse;
import com.systemdesign.ecommarketplace.auth.dto.LoginRequest;
import com.systemdesign.ecommarketplace.auth.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors src/modules/auth/auth.controller.ts. Public - no JwtAuthGuard. */
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
  public AuthResponse register(@Valid @RequestBody RegisterRequest dto) {
    return authService.register(dto);
  }

  @PostMapping("/login")
  @ResponseStatus(HttpStatus.OK)
  public AuthResponse login(@Valid @RequestBody LoginRequest dto) {
    return authService.login(dto);
  }
}
