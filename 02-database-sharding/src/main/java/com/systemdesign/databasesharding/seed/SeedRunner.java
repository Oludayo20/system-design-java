package com.systemdesign.databasesharding.seed;

import com.systemdesign.databasesharding.common.IdGeneratorService;
import com.systemdesign.databasesharding.sharding.ShardResolutionContext;
import com.systemdesign.databasesharding.sharding.ShardingStrategy;
import com.systemdesign.databasesharding.sharding.ShardingStrategyFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

/**
 * Inserts ~1000 synthetic users directly (no HTTP hop) against the shard
 * {@link JdbcTemplate}s so this can run standalone against the shards
 * started by {@code docker compose up -d}, then prints the resulting
 * per-shard distribution.
 *
 * Under the default hash strategy this should come out roughly even across
 * shards. If you flip {@code SHARDING_STRATEGY=range} before running this,
 * the same 1000 users - because their global IDs are generated sequentially
 * in time - will land almost entirely on whichever shard owns the current
 * id window, skewing hard toward one shard. That skew is the concrete
 * illustration of why range sharding needs care around ID distribution
 * while hash sharding doesn't.
 *
 * Ported from scripts/seed.ts as a Spring profile-gated
 * {@link CommandLineRunner} (rather than a separate script) so it can reuse
 * the same Spring-managed {@code JdbcTemplate} beans, {@link IdGeneratorService},
 * and {@link ShardingStrategyFactory} as the rest of the app instead of
 * duplicating shard-connection wiring. Activate it with:
 *
 * <pre>
 *   java -jar app.jar --spring.profiles.active=seed
 *   # or, via Maven:
 *   mvn spring-boot:run -Dspring-boot.run.profiles=seed
 * </pre>
 *
 * It prints its summary and then exits the process, mirroring the
 * standalone-script behavior of the original.
 */
@Component
@Profile("seed")
public class SeedRunner implements CommandLineRunner {

    private static final List<String> REGIONS = List.of("africa", "europe", "asia");
    private static final int USER_COUNT = 1000;
    private static final String SUFFIX_CHARS = "abcdefghijklmnopqrstuvwxyz0123456789";

    private final JdbcTemplate shard0JdbcTemplate;
    private final JdbcTemplate shard1JdbcTemplate;
    private final JdbcTemplate shard2JdbcTemplate;
    private final IdGeneratorService idGenerator;
    private final ConfigurableApplicationContext context;
    private final String strategyName;
    private final int shardCount;
    private final Random random = new Random();

    public SeedRunner(
            @Qualifier("shard0JdbcTemplate") JdbcTemplate shard0JdbcTemplate,
            @Qualifier("shard1JdbcTemplate") JdbcTemplate shard1JdbcTemplate,
            @Qualifier("shard2JdbcTemplate") JdbcTemplate shard2JdbcTemplate,
            IdGeneratorService idGenerator,
            ConfigurableApplicationContext context,
            @Value("${SHARDING_STRATEGY:hash}") String strategyName,
            @Value("${SHARD_COUNT:3}") int shardCount) {
        this.shard0JdbcTemplate = shard0JdbcTemplate;
        this.shard1JdbcTemplate = shard1JdbcTemplate;
        this.shard2JdbcTemplate = shard2JdbcTemplate;
        this.idGenerator = idGenerator;
        this.context = context;
        this.strategyName = strategyName;
        this.shardCount = shardCount;
    }

    @Override
    public void run(String... args) {
        List<JdbcTemplate> templates = List.of(shard0JdbcTemplate, shard1JdbcTemplate, shard2JdbcTemplate);
        ShardingStrategy strategy = ShardingStrategyFactory.create(strategyName, shardCount);

        System.out.printf("Seeding %d users using the \"%s\" strategy...%n", USER_COUNT, strategy.getName());

        int[] perShardInserted = new int[templates.size()];

        for (int i = 0; i < USER_COUNT; i++) {
            long id = idGenerator.nextId();
            String region = randomRegion();
            int shardIndex = strategy.resolveShard(id, new ShardResolutionContext(region));

            templates.get(shardIndex).update(
                    """
                    INSERT INTO users (id, email, display_name, region)
                    VALUES (?, ?, ?, ?)
                    ON CONFLICT (id) DO NOTHING
                    """,
                    id, randomEmail(i), "Seed User " + i, region
            );

            perShardInserted[shardIndex]++;
        }

        System.out.println("\nInserted (by this run):");
        for (int shardIndex = 0; shardIndex < perShardInserted.length; shardIndex++) {
            System.out.printf("  shard %d: %d rows%n", shardIndex, perShardInserted[shardIndex]);
        }

        System.out.println("\nActual row counts per shard (COUNT(*) after seeding):");
        long total = 0;
        for (int shardIndex = 0; shardIndex < templates.size(); shardIndex++) {
            Long count = templates.get(shardIndex).queryForObject("SELECT COUNT(*) FROM users", Long.class);
            long shardTotal = count == null ? 0 : count;
            total += shardTotal;
            System.out.printf("  shard %d: %d rows%n", shardIndex, shardTotal);
        }
        System.out.printf("  total: %d rows%n", total);

        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }

    private String randomEmail(int index) {
        return "seed-user-" + index + "-" + randomSuffix() + "@example.com";
    }

    private String randomSuffix() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            sb.append(SUFFIX_CHARS.charAt(random.nextInt(SUFFIX_CHARS.length())));
        }
        return sb.toString();
    }

    private String randomRegion() {
        return REGIONS.get(random.nextInt(REGIONS.size()));
    }
}
