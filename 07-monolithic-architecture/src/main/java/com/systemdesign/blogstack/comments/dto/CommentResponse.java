package com.systemdesign.blogstack.comments.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "A comment on a post.")
public record CommentResponse(
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID id,
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID postId,
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID userId,
        @Schema(example = "Great breakdown of the tradeoffs!") String body,
        @Schema(example = "2026-08-08T12:00:00") LocalDateTime createdAt) {
}
