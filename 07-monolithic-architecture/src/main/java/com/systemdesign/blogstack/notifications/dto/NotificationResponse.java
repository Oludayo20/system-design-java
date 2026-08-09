package com.systemdesign.blogstack.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "A notification.")
public record NotificationResponse(
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID id,
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID recipientId,
        @Schema(example = "Jane Doe commented on your post \"Why plain monoliths still ship products\"") String message,
        @Schema(example = "false") boolean read,
        @Schema(example = "2026-08-08T12:00:00") LocalDateTime createdAt) {
}
