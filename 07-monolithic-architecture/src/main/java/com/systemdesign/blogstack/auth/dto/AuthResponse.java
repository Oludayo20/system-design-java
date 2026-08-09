package com.systemdesign.blogstack.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "JWT access token and authenticated user summary.")
public record AuthResponse(
        @Schema(description = "JWT bearer token. Pass as Authorization: Bearer <token>.") String accessToken,
        UserSummary user) {

    @Schema(name = "UserSummary")
    public record UserSummary(
            @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") UUID id,
            @Schema(example = "jane.doe@example.com") String email,
            @Schema(example = "Jane Doe") String displayName) {
    }
}
