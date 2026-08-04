package com.systemdesign.ecommarketplace.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Mirrors src/modules/auth/dto/register.dto.ts. */
public record RegisterRequest(
    @Email @NotBlank String email,
    @NotBlank @Size(min = 8) String password,
    @NotBlank @Size(min = 2) String fullName) {}
