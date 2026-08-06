package com.systemdesign.ecommarketplace.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials.")
public record LoginRequest(
    @Schema(example = "ada@oja.dev") @Email @NotBlank String email,
    @Schema(example = "correct horse battery staple") @NotBlank String password) {}
