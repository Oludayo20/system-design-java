package com.systemdesign.legacyinmemory.infrastructure;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Java port of {@code infrastructure/sharding.js}'s {@code ShardRouter} - simulates splitting
 * one logical database across multiple physical shards, each an independent
 * {@link InMemoryDatabase}.
 *
 * <p>doc.md: "Instead of one huge database ... you split it into smaller databases", worked
 * example {@code userId % 3} for users 15, 230, 987, 1500.
 *
 * <p>Not used by the main API (the {@code identity}/{@code catalog}/{@code basket}/
 * {@code ordering} modules all share a single unsharded {@link InMemoryDatabase}, exactly as
 * in {@code bootstrap.js}, which never imports {@code sharding.js}); this class is only
 * exercised standalone in the demo runner's "Act 3", matching {@code scripts/demo.js}.
 */
public class ShardRouter {

    /** Mirrors JS {@code Number.MAX_SAFE_INTEGER} (2^53 - 1), used by the range strategy. */
    private static final long MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    private final int numShards;
    private final String strategy; // "hash" or "range"
    private final List<InMemoryDatabase> shards;

    public ShardRouter(int numShards, String strategy) {
        this.numShards = numShards;
        this.strategy = strategy;
        this.shards = new ArrayList<>(numShards);
        for (int i = 0; i < numShards; i++) {
            shards.add(new InMemoryDatabase("shard-" + i));
        }
    }

    public int shardIndexFor(long userId) {
        if ("hash".equals(strategy)) {
            return (int) (userId % numShards); // doc.md Strategy 2: Hashing
        }
        // doc.md Strategy 1: range-based (e.g. 1-3,000,000 -> Shard 1, etc.)
        long rangeSize = (long) Math.ceil((double) MAX_SAFE_INTEGER / numShards);
        return (int) Math.min(numShards - 1, Math.floor((double) userId / rangeSize));
    }

    public InMemoryDatabase dbFor(long userId) {
        return shards.get(shardIndexFor(userId));
    }

    public int writeUser(long userId, Map<String, Object> record) {
        int shardIndex = shardIndexFor(userId);
        shards.get(shardIndex).insert("users", record);
        return shardIndex;
    }

    @SuppressWarnings("unchecked")
    public ShardReadResult readUser(long userId) {
        int shardIndex = shardIndexFor(userId);
        Optional<Map<String, Object>> record = shards.get(shardIndex)
                .find("users", (Map<String, Object> u) -> Long.valueOf(userId).equals(toLong(u.get("id"))));
        return new ShardReadResult(shardIndex, record.orElse(null));
    }

    private static Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return null;
    }

    public List<Map<String, Object>> distributionSummary() {
        List<Map<String, Object>> summary = new ArrayList<>();
        for (int i = 0; i < shards.size(); i++) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("shard", i);
            entry.put("users", shards.get(i).table("users").size());
            summary.add(entry);
        }
        return summary;
    }

    public record ShardReadResult(int shardIndex, Map<String, Object> record) {
    }
}
