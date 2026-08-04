package com.systemdesign.legacyinmemory.modules.identity;

/** {@code { token, user }} - matches the shape returned by {@code identity.module.js#login}. */
public record LoginResult(String token, UserView user) {
}
