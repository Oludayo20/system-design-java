package com.systemdesign.databasesharding.sharding.strategies;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Ported from range-sharding.strategy.spec.ts. */
class RangeShardingStrategyTest {

    // Mirrors the doc's example: Users 1-3M -> Shard 0, 3M-6M -> Shard 1, 6M+ -> Shard 2
    private final RangeShardingStrategy strategy =
            new RangeShardingStrategy(List.of(3_000_000.0, 6_000_000.0, Double.POSITIVE_INFINITY));

    @ParameterizedTest(name = "routes id {0} to shard {1}")
    @CsvSource({
            "1, 0",
            "3000000, 0",
            "3000001, 1",
            "6000000, 1",
            "6000001, 2",
            "15345678, 2"
    })
    void routesIdToShard(long id, int expectedShard) {
        assertEquals(expectedShard, strategy.resolveShard(id));
    }

    @Test
    void acceptsNumericStrings() {
        assertEquals(1, strategy.resolveShard("4000000"));
    }

    @Test
    void rejectsNonNumericKeys() {
        assertThrows(IllegalArgumentException.class, () -> strategy.resolveShard("not-a-number"));
    }

    @Test
    void rejectsBoundariesThatAreNotStrictlyAscending() {
        assertThrows(IllegalArgumentException.class, () -> new RangeShardingStrategy(List.of(100.0, 100.0)));
        assertThrows(IllegalArgumentException.class, () -> new RangeShardingStrategy(List.of(100.0, 50.0)));
    }

    @Test
    void rejectsAnEmptyBoundaryList() {
        assertThrows(IllegalArgumentException.class, () -> new RangeShardingStrategy(List.of()));
    }
}
