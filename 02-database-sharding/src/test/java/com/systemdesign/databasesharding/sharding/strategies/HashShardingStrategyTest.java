package com.systemdesign.databasesharding.sharding.strategies;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ported from hash-sharding.strategy.spec.ts. */
class HashShardingStrategyTest {

    @Nested
    class TheDocWorkedExample {
        private final HashShardingStrategy strategy = new HashShardingStrategy(3);

        @ParameterizedTest(name = "routes userId {0} to shard {1}")
        @CsvSource({
                "15, 0",
                "230, 2",
                "987, 0",
                "1500, 0"
        })
        void routesUserIdToShard(long userId, int expectedShard) {
            assertEquals(expectedShard, strategy.resolveShard(userId));
        }
    }

    @Test
    void isDeterministicSameKeyAlwaysResolvesToSameShard() {
        HashShardingStrategy strategy = new HashShardingStrategy(5);
        int shard = strategy.resolveShard(42);
        for (int i = 0; i < 10; i++) {
            assertEquals(shard, strategy.resolveShard(42));
        }
    }

    @Test
    void alwaysReturnsAnIndexWithinShardCount() {
        int shardCount = 4;
        HashShardingStrategy strategy = new HashShardingStrategy(shardCount);
        long[] userIds = {0, 1, 3, 99, 100_000, 999_999_937L};
        for (long userId : userIds) {
            int shard = strategy.resolveShard(userId);
            assertTrue(shard >= 0);
            assertTrue(shard < shardCount);
        }
    }

    @Test
    void hashesStringKeysToAStableInRangeShard() {
        HashShardingStrategy strategy = new HashShardingStrategy(3);
        int first = strategy.resolveShard("user@example.com");
        int second = strategy.resolveShard("user@example.com");
        assertEquals(first, second);
        assertTrue(first >= 0);
        assertTrue(first < 3);
    }

    @Test
    void distributesALargeKeySetRoughlyEvenlyAcrossShards() {
        int shardCount = 3;
        HashShardingStrategy strategy = new HashShardingStrategy(shardCount);
        int[] counts = new int[shardCount];

        for (long userId = 1; userId <= 9000; userId++) {
            counts[strategy.resolveShard(userId)]++;
        }

        // Sequential integers mod 3 split perfectly evenly.
        for (int count : counts) {
            assertEquals(3000, count);
        }
    }

    @Test
    void rejectsAnInvalidShardCount() {
        assertThrows(IllegalArgumentException.class, () -> new HashShardingStrategy(0));
    }
}
