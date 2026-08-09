package com.systemdesign.bookhive.notification.notifications.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.Map;

public record NotificationRequest(
        @Schema(example = "order.created")
        @NotBlank String type,

        @Schema(example = "3b1f8e2a-1c2d-4e3f-9a0b-123456789abc")
        @NotBlank String userId,

        @Schema(example = "Order abc123 placed for 2x book def456")
        @NotBlank String message,

        Map<String, Object> metadata) {
}
