package com.systemdesign.blogstack.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Current user profile.")
public record UserResponse(
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID id,
        @Schema(example = "jane.doe@example.com") String email,
        @Schema(example = "Jane Doe") String displayName,
        @Schema(example = "2026-08-08T12:00:00") LocalDateTime createdAt) {
}
