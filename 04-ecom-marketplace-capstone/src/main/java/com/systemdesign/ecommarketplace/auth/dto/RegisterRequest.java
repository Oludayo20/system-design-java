package com.systemdesign.ecommarketplace.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "New user registration.")
public record RegisterRequest(
    @Schema(example = "ada@oja.dev") @Email @NotBlank String email,
    @Schema(example = "correct horse battery staple") @NotBlank @Size(min = 8) String password,
    @Schema(example = "Ada Lovelace") @NotBlank @Size(min = 2) String fullName) {}
