package com.systemdesign.modularmonolith.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Login credentials.")
public record LoginRequest(
        @Schema(example = "jane.doe@example.com") @NotBlank @Email String email,
        @Schema(example = "S3curePassword!") @NotBlank String password) {
}
