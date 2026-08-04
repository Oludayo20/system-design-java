package com.systemdesign.databasesharding.sharding;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/**
 * Ported from shard-manager.service.spec.ts.
 *
 * A {@link JdbcTemplate} never opens a connection until a query actually
 * runs, so - exactly like the original's note about {@code pg.Pool} - this
 * test exercises real {@link ShardManagerService} construction and routing
 * logic without needing a live Postgres instance. The backing
 * {@link DataSource} is a Mockito mock that is never invoked.
 */
class ShardManagerServiceTest {

    private ShardManagerService buildService() {
        JdbcTemplate shard0 = new JdbcTemplate(mock(DataSource.class));
        JdbcTemplate shard1 = new JdbcTemplate(mock(DataSource.class));
        JdbcTemplate shard2 = new JdbcTemplate(mock(DataSource.class));
        return new ShardManagerService(shard0, shard1, shard2, 3, "hash");
    }

    @Test
    void createsOneTemplatePerConfiguredShard() {
        ShardManagerService service = buildService();
        assertEquals(3, service.getShardCount());
    }

    @Test
    void resolvesTheDocWorkedExampleThroughTheActiveHashStrategy() {
        ShardManagerService service = buildService();
        assertEquals(0, service.resolveShardIndex(15L));
        assertEquals(2, service.resolveShardIndex(230L));
        assertEquals(0, service.resolveShardIndex(987L));
        assertEquals(0, service.resolveShardIndex(1500L));
    }

    @Test
    void getTemplateForKeyReturnsExactlyOneTemplateNeverACollection() {
        ShardManagerService service = buildService();
        JdbcTemplate template = service.getTemplateForKey(15L);
        assertNotNull(template);
    }

    @Test
    void getAllTemplatesExposesAllShardsForTheDebugDistributionUseCaseOnly() {
        ShardManagerService service = buildService();
        assertEquals(3, service.getAllTemplates().size());
    }
}
