package com.systemdesign.databasesharding.users.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** Mirrors shard-distribution-response.dto.ts. */
public record ShardDistributionResponseDto(
        @Schema(description = "Active sharding strategy at the time this ran.") String strategy,
        List<ShardCountDto> shards,
        long totalUsers
) {
}
