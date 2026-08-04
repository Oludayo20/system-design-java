package com.systemdesign.databasesharding.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Defines one {@link DataSource} + {@link JdbcTemplate} per physical shard,
 * configured from {@code SHARD_<index>_*} environment variables - the same
 * variables read by {@code src/config/configuration.ts} in the original.
 *
 * Mirrors the {@code pg.Pool} instances created in shard-manager.service.ts:
 * a max pool size of 10 and an {@code application_name} of
 * {@code shard-router-shard-<index>} per shard, for easy identification in
 * {@code pg_stat_activity}.
 *
 * The topology is fixed at three shards (shard0/shard1/shard2), matching
 * this project's three-Postgres-container docker-compose setup and the
 * {@code SHARDING_STRATEGY} default. {@code SHARD_COUNT} (read separately
 * by {@link com.systemdesign.databasesharding.sharding.ShardManagerService})
 * only affects the sharding *strategy's* shard-count math, not how many
 * DataSource beans exist.
 */
@Configuration
public class DataSourceConfig {

    @Bean(name = "shard0DataSource")
    public DataSource shard0DataSource(
            @Value("${SHARD_0_HOST:localhost}") String host,
            @Value("${SHARD_0_PORT:5432}") int port,
            @Value("${SHARD_0_DB:shard_0}") String database,
            @Value("${SHARD_0_USER:postgres}") String user,
            @Value("${SHARD_0_PASSWORD:postgres}") String password) {
        return buildDataSource(host, port, database, user, password, 0);
    }

    @Bean(name = "shard1DataSource")
    public DataSource shard1DataSource(
            @Value("${SHARD_1_HOST:localhost}") String host,
            @Value("${SHARD_1_PORT:5433}") int port,
            @Value("${SHARD_1_DB:shard_1}") String database,
            @Value("${SHARD_1_USER:postgres}") String user,
            @Value("${SHARD_1_PASSWORD:postgres}") String password) {
        return buildDataSource(host, port, database, user, password, 1);
    }

    @Bean(name = "shard2DataSource")
    public DataSource shard2DataSource(
            @Value("${SHARD_2_HOST:localhost}") String host,
            @Value("${SHARD_2_PORT:5434}") int port,
            @Value("${SHARD_2_DB:shard_2}") String database,
            @Value("${SHARD_2_USER:postgres}") String user,
            @Value("${SHARD_2_PASSWORD:postgres}") String password) {
        return buildDataSource(host, port, database, user, password, 2);
    }

    @Bean(name = "shard0JdbcTemplate")
    public JdbcTemplate shard0JdbcTemplate(@Qualifier("shard0DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "shard1JdbcTemplate")
    public JdbcTemplate shard1JdbcTemplate(@Qualifier("shard1DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "shard2JdbcTemplate")
    public JdbcTemplate shard2JdbcTemplate(@Qualifier("shard2DataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    private DataSource buildDataSource(
            String host, int port, String database, String user, String password, int shardIndex) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        dataSource.setUsername(user);
        dataSource.setPassword(password);
        dataSource.setMaximumPoolSize(10);
        dataSource.setPoolName("shard-router-shard-" + shardIndex);
        dataSource.addDataSourceProperty("ApplicationName", "shard-router-shard-" + shardIndex);
        return dataSource;
    }
}
