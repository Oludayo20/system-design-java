package com.systemdesign.databasesharding.sharding;

/**
 * Every strategy answers the same question - "given this key, which shard
 * index (0..N-1) is responsible for it?" - using a different rule. The rest
 * of the application never needs to know which rule is active; it only
 * calls {@link #resolveShard} through the active strategy.
 *
 * Mirrors sharding-strategy.interface.ts.
 */
public interface ShardingStrategy {

    /** Human-readable name, mostly useful for logging/debugging. */
    String getName();

    /**
     * Resolve the shard index (0-based) that owns {@code key}.
     *
     * @param key     numeric ({@code Long}/{@code Integer}) or string identifier (e.g. a userId)
     * @param context optional extra data some strategies need (e.g. region); may be {@code null}
     */
    int resolveShard(Object key, ShardResolutionContext context);

    /** Convenience overload equivalent to calling with an undefined/absent context. */
    default int resolveShard(Object key) {
        return resolveShard(key, null);
    }
}
