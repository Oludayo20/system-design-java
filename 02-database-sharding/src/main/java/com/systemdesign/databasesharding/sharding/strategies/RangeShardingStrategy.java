package com.systemdesign.databasesharding.sharding.strategies;

import com.systemdesign.databasesharding.sharding.ShardResolutionContext;
import com.systemdesign.databasesharding.sharding.ShardingStrategy;

import java.util.List;

/**
 * Range-based sharding: bucket numeric IDs into configurable contiguous
 * windows, e.g. Users 1-3,000,000 -&gt; Shard 0, 3,000,001-6,000,000 -&gt;
 * Shard 1, 6,000,001+ -&gt; Shard 2.
 *
 * Simple to reason about and easy to debug ("where does user 4.2M live?
 * shard 1"), but it concentrates all *new* writes on whichever shard owns
 * the current upper end of the range. If IDs are assigned sequentially (the
 * common case for auto-incrementing primary keys), the newest shard becomes
 * a hot shard while older shards sit comparatively idle.
 *
 * Boundaries are given as ascending upper bounds. The last boundary should
 * usually be {@link Double#POSITIVE_INFINITY} so every key resolves to a
 * shard.
 *
 * Example: boundaries = [3_000_000, 6_000_000, Infinity]
 *   id &lt;= 3,000,000                -&gt; shard 0
 *   3,000,000 &lt; id &lt;= 6,000,000    -&gt; shard 1
 *   id &gt; 6,000,000                 -&gt; shard 2
 *
 * Mirrors range-sharding.strategy.ts. {@code double} is used for boundaries
 * (instead of {@code long}) purely so {@code Infinity} can be represented
 * exactly as it is in the TS original.
 */
public class RangeShardingStrategy implements ShardingStrategy {

    private final double[] upperBounds;

    public RangeShardingStrategy(List<Double> upperBounds) {
        if (upperBounds.isEmpty()) {
            throw new IllegalArgumentException("upperBounds must contain at least one boundary");
        }
        for (int i = 1; i < upperBounds.size(); i++) {
            if (upperBounds.get(i) <= upperBounds.get(i - 1)) {
                throw new IllegalArgumentException("upperBounds must be strictly ascending");
            }
        }
        this.upperBounds = new double[upperBounds.size()];
        for (int i = 0; i < upperBounds.size(); i++) {
            this.upperBounds[i] = upperBounds.get(i);
        }
    }

    @Override
    public String getName() {
        return "range";
    }

    @Override
    public int resolveShard(Object key, ShardResolutionContext context) {
        double numericKey;
        if (key instanceof Number number) {
            numericKey = number.doubleValue();
        } else {
            try {
                numericKey = Double.parseDouble(String.valueOf(key));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "RangeShardingStrategy requires a numeric key, got \"" + key + "\"");
            }
        }

        for (int shardIndex = 0; shardIndex < upperBounds.length; shardIndex++) {
            if (numericKey <= upperBounds[shardIndex]) {
                return shardIndex;
            }
        }

        // Key exceeds every configured boundary - route to the last shard.
        return upperBounds.length - 1;
    }
}
