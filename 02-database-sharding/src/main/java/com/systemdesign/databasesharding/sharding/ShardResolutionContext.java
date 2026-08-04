package com.systemdesign.databasesharding.sharding;

/**
 * A shard key decides which physical shard stores (or owns) a given record.
 *
 * Mirrors sharding-strategy.interface.ts's {@code ShardResolutionContext}.
 * Only {@link GeoShardingStrategy} in the {@code strategies} package reads
 * {@code region}; every other strategy ignores it.
 */
public record ShardResolutionContext(String region) {
}
