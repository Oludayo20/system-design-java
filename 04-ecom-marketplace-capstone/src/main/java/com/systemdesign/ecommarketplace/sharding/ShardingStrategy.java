package com.systemdesign.ecommarketplace.sharding;

/**
 * Mirrors src/sharding/sharding-strategy.interface.ts.
 *
 * <p>A shard key decides which physical shard owns a given record. The rest
 * of the application never needs to know how the decision is made - it only
 * calls resolveShard on whichever strategy is active.
 */
public interface ShardingStrategy {

  /** Human-readable name, useful for logging. */
  String getName();

  /** Number of shards this strategy distributes across. */
  int getShardCount();

  /** Resolve the shard index (0-based) that owns the string key (e.g. a userId). */
  int resolveShard(String key);

  /** Resolve the shard index (0-based) that owns the numeric key. */
  int resolveShard(long key);
}
