package com.systemdesign.databasesharding.users;

import java.time.OffsetDateTime;

/** Mirrors user.types.ts's {@code UserRow} - the raw shape returned from a shard query. */
public record UserRow(long id, String email, String displayName, String region, OffsetDateTime createdAt) {
}
