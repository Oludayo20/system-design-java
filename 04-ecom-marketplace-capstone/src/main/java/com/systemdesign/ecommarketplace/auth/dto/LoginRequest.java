package com.systemdesign.ecommarketplace.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Mirrors src/modules/auth/dto/login.dto.ts. */
public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
