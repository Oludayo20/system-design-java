package com.systemdesign.databasesharding.sharding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Owns one {@link JdbcTemplate} (backed by its own connection pool) per
 * physical shard and routes a lookup key to exactly one of them via the
 * active {@link ShardingStrategy}.
 *
 * This is the piece that makes "one logical database" possible: every other
 * part of the app talks to {@code ShardManagerService}, never to a shard's
 * pool directly, so nobody can accidentally query the wrong shard - or,
 * worse, every shard - for what should be a single-record lookup.
 *
 * {@link #getAllTemplates()} exists solely for the distribution/debug
 * endpoint, which is the one legitimate cross-shard operation in this
 * codebase (an operational "how balanced are we?" check, not a
 * request-path query).
 *
 * Mirrors shard-manager.service.ts. The physical topology is fixed at
 * three shards (matching the project's docker-compose topology and the
 * {@code shard0JdbcTemplate}/{@code shard1JdbcTemplate}/{@code
 * shard2JdbcTemplate} beans in {@code DataSourceConfig}); {@code
 * SHARD_COUNT} only feeds the active strategy's shard-count math (e.g. the
 * hash modulo divisor or how many range boundaries to generate), exactly as
 * it does for the TypeScript original when its shard array and shardCount
 * happen to be configured consistently.
 */
@Slf4j
@Service
public class ShardManagerService {

    private final List<JdbcTemplate> templates;
    private final ShardingStrategy strategy;

    public ShardManagerService(
            @Qualifier("shard0JdbcTemplate") JdbcTemplate shard0JdbcTemplate,
            @Qualifier("shard1JdbcTemplate") JdbcTemplate shard1JdbcTemplate,
            @Qualifier("shard2JdbcTemplate") JdbcTemplate shard2JdbcTemplate,
            @Value("${SHARD_COUNT:3}") int shardCount,
            @Value("${SHARDING_STRATEGY:hash}") String strategyName) {
        this.templates = List.of(shard0JdbcTemplate, shard1JdbcTemplate, shard2JdbcTemplate);
        this.strategy = ShardingStrategyFactory.create(strategyName, shardCount);
        log.info("Sharding strategy \"{}\" active across {} shard(s)", strategy.getName(), templates.size());
    }

    public int getShardCount() {
        return templates.size();
    }

    public String getStrategyName() {
        return strategy.getName();
    }

    /** Resolve which shard index owns {@code key} without touching the network. */
    public int resolveShardIndex(Object key, ShardResolutionContext context) {
        return strategy.resolveShard(key, context);
    }

    public int resolveShardIndex(Object key) {
        return resolveShardIndex(key, null);
    }

    /**
     * The only way normal query code should reach a database: resolve the
     * single shard that owns {@code key} and hand back its
     * {@link JdbcTemplate}. There is no "give me template N" method exposed
     * for arbitrary use - callers cannot scatter-gather by accident because
     * they never see the other templates.
     */
    public JdbcTemplate getTemplateForKey(Object key, ShardResolutionContext context) {
        int shardIndex = resolveShardIndex(key, context);
        return templates.get(shardIndex);
    }

    public JdbcTemplate getTemplateForKey(Object key) {
        return getTemplateForKey(key, null);
    }

    /**
     * Cross-shard escape hatch. Intentionally named and documented as such -
     * reach for this only from operational/debug code (e.g. counting rows
     * per shard), never from a hot request path.
     */
    public List<JdbcTemplate> getAllTemplates() {
        return templates;
    }
}
