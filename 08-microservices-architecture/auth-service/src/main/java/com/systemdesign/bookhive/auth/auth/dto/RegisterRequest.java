package com.systemdesign.bookhive.auth.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Schema(example = "reader@bookhive.dev")
        @NotBlank @Email String email,

        @Schema(example = "hunter22", minLength = 8)
        @NotBlank @Size(min = 8) String password,

        @Schema(example = "Ada Lovelace")
        @NotBlank String fullName) {
}
