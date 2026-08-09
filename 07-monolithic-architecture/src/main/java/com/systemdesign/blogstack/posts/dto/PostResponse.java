package com.systemdesign.blogstack.posts.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "A blog post.")
public record PostResponse(
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID id,
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID userId,
        @Schema(example = "Why plain monoliths still ship products") String title,
        @Schema(example = "A monolith is just one app, one repo, one deploy...") String body,
        @Schema(example = "2026-08-08T12:00:00") LocalDateTime createdAt) {
}
