package com.systemdesign.databasesharding.sharding.strategies;

import com.systemdesign.databasesharding.sharding.ShardResolutionContext;
import com.systemdesign.databasesharding.sharding.ShardingStrategy;

/**
 * Hash-based sharding: {@code hash(key) % shardCount}.
 *
 * This is the default strategy used across the demo because it distributes
 * records evenly across shards regardless of insertion order - unlike range
 * sharding, it doesn't create a "hot" shard while IDs are climbing through a
 * single numeric window.
 *
 * For numeric keys (the common case - userId) we deliberately do NOT hash
 * the number first: modulo is already a perfectly uniform distribution over
 * sequential integers, and using the raw value keeps the doc's worked
 * example reproducible bit-for-bit: {@code userId % 3}.
 *   15   % 3 = 0
 *   230  % 3 = 2
 *   987  % 3 = 0
 *   1500 % 3 = 0
 *
 * For string keys (e.g. an email used as a shard key) we first fold the
 * string down to a stable 32-bit integer via djb2, then apply the same
 * modulo. djb2 is not cryptographic - it's chosen because it's simple,
 * deterministic across processes/languages, and fast.
 *
 * Mirrors hash-sharding.strategy.ts, including the exact djb2 bit-mixing:
 * Java's {@code int} multiplication/XOR wraps modulo 2^32 the same way the
 * original's {@code (hash * 33) ^ charCode} does once JS's {@code ^}
 * coerces both operands to Int32 - so string hashes match bit-for-bit too.
 */
public class HashShardingStrategy implements ShardingStrategy {

    private final int shardCount;

    public HashShardingStrategy(int shardCount) {
        if (shardCount < 1) {
            throw new IllegalArgumentException("shardCount must be >= 1");
        }
        this.shardCount = shardCount;
    }

    @Override
    public String getName() {
        return "hash";
    }

    @Override
    public int resolveShard(Object key, ShardResolutionContext context) {
        long numericKey = (key instanceof Number number) ? number.longValue() : djb2(String.valueOf(key));
        return mod(numericKey, shardCount);
    }

    private long djb2(String input) {
        int hash = 5381;
        for (int i = 0; i < input.length(); i++) {
            hash = (hash * 33) ^ input.charAt(i);
        }
        // Equivalent of JS's `>>> 0`: force an unsigned 32-bit integer.
        return Integer.toUnsignedLong(hash);
    }

    /** Normalize to a non-negative result, mirroring the TS helper of the same name. */
    private int mod(long value, int divisor) {
        return (int) (((value % divisor) + divisor) % divisor);
    }
}
