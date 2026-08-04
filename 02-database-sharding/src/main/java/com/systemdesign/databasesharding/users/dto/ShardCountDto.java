package com.systemdesign.databasesharding.users.dto;

/** Mirrors the {@code ShardCountDto} declared inside shard-distribution-response.dto.ts. */
public record ShardCountDto(int shardIndex, long userCount) {
}
