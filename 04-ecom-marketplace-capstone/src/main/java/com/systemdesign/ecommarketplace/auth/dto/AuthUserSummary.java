package com.systemdesign.ecommarketplace.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "Authenticated user summary (no password).")
public record AuthUserSummary(
    UUID id,
    @Schema(example = "ada@oja.dev") String email,
    @Schema(example = "Ada Lovelace") String fullName) {}
