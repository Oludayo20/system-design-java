package com.systemdesign.legacyinmemory.modules.identity;

import java.util.List;

/**
 * The public-facing projection of a {@link User} - {@code { id, email, roles }}, deliberately
 * excluding {@code passwordHash}. Matches what {@code identity.module.js}'s {@code register}
 * and {@code login} return to callers instead of the raw stored record.
 */
public record UserView(int id, String email, List<String> roles) {
}
