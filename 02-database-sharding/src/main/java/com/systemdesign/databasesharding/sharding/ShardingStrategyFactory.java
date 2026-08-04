package com.systemdesign.databasesharding.sharding;

import com.systemdesign.databasesharding.sharding.strategies.GeoShardingStrategy;
import com.systemdesign.databasesharding.sharding.strategies.HashShardingStrategy;
import com.systemdesign.databasesharding.sharding.strategies.RangeShardingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds the active {@link ShardingStrategy} from config. Mirrors
 * sharding-strategy.factory.ts, including its default range boundaries and
 * geo map.
 */
public final class ShardingStrategyFactory {

    /** Region -&gt; shard index, mirroring the doc's Africa/Europe/Asia example. */
    private static final Map<String, Integer> DEFAULT_GEO_MAP = Map.of(
            "africa", 0,
            "europe", 1,
            "asia", 2
    );

    private ShardingStrategyFactory() {
    }

    public static ShardingStrategy create(String strategyName, int shardCount) {
        if (strategyName == null) {
            throw new IllegalArgumentException("Unknown sharding strategy: null");
        }
        return switch (strategyName) {
            case "hash" -> new HashShardingStrategy(shardCount);
            case "range" -> new RangeShardingStrategy(buildDefaultRangeBoundaries(shardCount));
            case "geo" -> new GeoShardingStrategy(DEFAULT_GEO_MAP);
            default -> throw new IllegalArgumentException("Unknown sharding strategy: " + strategyName);
        };
    }

    /**
     * Range boundaries mirroring the doc's Oja example: Shard 0 = users
     * 1-10M, Shard 1 = 10M-20M, Shard 2 = 20M-30M. Extended generically for
     * {@code shardCount} shards of 10M users each.
     */
    private static List<Double> buildDefaultRangeBoundaries(int shardCount) {
        List<Double> boundaries = new ArrayList<>();
        for (int i = 1; i < shardCount; i++) {
            boundaries.add(i * 10_000_000.0);
        }
        boundaries.add(Double.POSITIVE_INFINITY);
        return boundaries;
    }
}
