package com.systemdesign.library.members.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateMemberRequest(
        @Schema(example = "Ada Lovelace") @NotBlank String name,
        @Schema(example = "ada@example.com") @NotBlank @Email String email) {
}
