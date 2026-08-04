package com.systemdesign.databasesharding.users;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Plain SQL against whichever {@link JdbcTemplate} the caller already
 * resolved. This class has no idea shards exist - it just runs queries
 * against a JDBC connection pool. All shard-routing decisions happen one
 * layer up, in {@link UsersService}, via {@code ShardManagerService}.
 * Keeping that separation is what makes it structurally impossible for a
 * query here to accidentally hit more than one shard.
 *
 * Mirrors users.repository.ts.
 */
@Repository
public class UsersRepository {

    private static final RowMapper<UserRow> USER_ROW_MAPPER = (rs, rowNum) -> new UserRow(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getString("display_name"),
            rs.getString("region"),
            rs.getObject("created_at", OffsetDateTime.class)
    );

    public UserRow insert(JdbcTemplate jdbcTemplate, long id, String email, String displayName, String region) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO users (id, email, display_name, region)
                VALUES (?, ?, ?, ?)
                RETURNING id, email, display_name, region, created_at
                """,
                USER_ROW_MAPPER,
                id, email, displayName, region
        );
    }

    public UserRow findById(JdbcTemplate jdbcTemplate, long id) {
        List<UserRow> rows = jdbcTemplate.query(
                "SELECT id, email, display_name, region, created_at FROM users WHERE id = ?",
                USER_ROW_MAPPER,
                id
        );
        return rows.isEmpty() ? null : rows.get(0);
    }

    public long count(JdbcTemplate jdbcTemplate) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        return count == null ? 0 : count;
    }
}
