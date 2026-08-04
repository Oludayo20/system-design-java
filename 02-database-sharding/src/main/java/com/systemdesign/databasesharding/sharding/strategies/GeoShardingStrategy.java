package com.systemdesign.databasesharding.sharding.strategies;

import com.systemdesign.databasesharding.sharding.ShardResolutionContext;
import com.systemdesign.databasesharding.sharding.ShardingStrategy;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Geography-based sharding: map a region code to a shard index via a lookup
 * table, e.g. Africa -&gt; Shard 0, Europe -&gt; Shard 1, Asia -&gt; Shard 2.
 *
 * Beyond distributing load, this strategy also reduces latency: a user in
 * Lagos gets served by a shard that can physically sit in a nearby data
 * center rather than one on another continent.
 *
 * Trade-off: traffic/data volume rarely splits evenly across regions, so
 * this can produce lopsided shards unless the regions themselves are
 * roughly comparable in size (or a region is further split later).
 *
 * Mirrors geo-sharding.strategy.ts.
 */
public class GeoShardingStrategy implements ShardingStrategy {

    private final Map<String, Integer> regionToShard;
    private final int defaultShard;

    public GeoShardingStrategy(Map<String, Integer> regionToShard) {
        this(regionToShard, 0);
    }

    public GeoShardingStrategy(Map<String, Integer> regionToShard, int defaultShard) {
        this.regionToShard = new LinkedHashMap<>(regionToShard);
        this.defaultShard = defaultShard;
    }

    @Override
    public String getName() {
        return "geo";
    }

    @Override
    public int resolveShard(Object key, ShardResolutionContext context) {
        String region = context != null ? context.region() : null;
        if (region == null || region.isEmpty()) {
            throw new IllegalStateException("GeoShardingStrategy requires a region in the resolution context");
        }

        String normalizedRegion = region.trim().toLowerCase();
        for (Map.Entry<String, Integer> entry : regionToShard.entrySet()) {
            if (entry.getKey().toLowerCase().equals(normalizedRegion)) {
                return entry.getValue();
            }
        }

        return defaultShard;
    }
}
