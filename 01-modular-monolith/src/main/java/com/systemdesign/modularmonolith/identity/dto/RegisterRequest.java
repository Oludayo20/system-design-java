package com.systemdesign.modularmonolith.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Mirrors {@code register.dto.ts}. */
public record RegisterRequest(
        @NotBlank @Email String email,
        @NotNull @Size(min = 8) String password,
        @NotBlank String fullName
) {
}
