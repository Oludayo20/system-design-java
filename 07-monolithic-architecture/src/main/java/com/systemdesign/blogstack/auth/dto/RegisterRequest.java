package com.systemdesign.blogstack.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "New account registration payload.")
public record RegisterRequest(
        @Schema(example = "jane.doe@example.com") @NotBlank @Email String email,
        @Schema(example = "S3curePassword!", minLength = 8) @NotNull @Size(min = 8) String password,
        @Schema(example = "Jane Doe") @NotBlank String displayName) {
}
