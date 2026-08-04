package com.systemdesign.databasesharding.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** Mirrors user-response.dto.ts. */
public record UserResponseDto(
        @Schema(example = "1927841923837952") String id,
        String email,
        String displayName,
        String region,
        String createdAt,
        @Schema(description = "Shard index that owns this record, for demo transparency.") int shardIndex
) {
}
