package com.systemdesign.ecommarketplace.common;

/**
 * Mirrors src/common/jwt-payload.interface.ts.
 *
 * sub - the userId, which is also the shard key. Kept minimal on purpose
 * (see AuthService): the JWT only needs to identify the user and their
 * email, everything else is looked up per-request from the correct shard.
 */
public record JwtPayload(String sub, String email) {}
