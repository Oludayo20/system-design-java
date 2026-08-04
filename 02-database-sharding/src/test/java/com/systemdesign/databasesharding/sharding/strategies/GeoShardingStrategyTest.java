package com.systemdesign.databasesharding.sharding.strategies;

import com.systemdesign.databasesharding.sharding.ShardResolutionContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Ported from geo-sharding.strategy.spec.ts. */
class GeoShardingStrategyTest {

    private final GeoShardingStrategy strategy = new GeoShardingStrategy(Map.of(
            "africa", 0,
            "europe", 1,
            "asia", 2
    ));

    @ParameterizedTest(name = "routes region {0} to shard {1}")
    @CsvSource({
            "africa, 0",
            "europe, 1",
            "asia, 2"
    })
    void routesRegionToShard(String region, int expectedShard) {
        assertEquals(expectedShard, strategy.resolveShard(1, new ShardResolutionContext(region)));
    }

    @Test
    void isCaseInsensitive() {
        assertEquals(0, strategy.resolveShard(1, new ShardResolutionContext("Africa")));
        assertEquals(1, strategy.resolveShard(1, new ShardResolutionContext("EUROPE")));
    }

    @Test
    void fallsBackToTheDefaultShardForAnUnknownRegion() {
        assertEquals(0, strategy.resolveShard(1, new ShardResolutionContext("antarctica")));
    }

    @Test
    void throwsWhenNoRegionIsProvided() {
        assertThrows(IllegalStateException.class, () -> strategy.resolveShard(1));
        assertThrows(IllegalStateException.class, () -> strategy.resolveShard(1, new ShardResolutionContext(null)));
    }

    @Test
    void honorsACustomDefaultShard() {
        GeoShardingStrategy customDefault = new GeoShardingStrategy(Map.of("africa", 0, "europe", 1), 1);
        assertEquals(1, customDefault.resolveShard(1, new ShardResolutionContext("oceania")));
    }
}
