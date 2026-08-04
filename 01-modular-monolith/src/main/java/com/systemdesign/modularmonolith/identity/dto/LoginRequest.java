package com.systemdesign.modularmonolith.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Mirrors {@code login.dto.ts}. */
public record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}
