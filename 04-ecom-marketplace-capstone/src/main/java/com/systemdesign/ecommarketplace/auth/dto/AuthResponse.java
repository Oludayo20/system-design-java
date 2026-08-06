package com.systemdesign.ecommarketplace.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access token and user summary.")
public record AuthResponse(
        @Schema(description = "JWT bearer token for protected routes.") String accessToken,
        AuthUserSummary user) {}
