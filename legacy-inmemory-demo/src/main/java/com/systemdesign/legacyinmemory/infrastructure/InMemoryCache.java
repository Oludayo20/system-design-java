package com.systemdesign.legacyinmemory.infrastructure;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Java port of {@code infrastructure/cache.js}'s {@code Cache} class - a tiny in-memory
 * stand-in for Redis. doc.md: "Redis here is used for things like: Sessions, Cache, Shopping
 * Cart, OTP, Rate Limiting, Frequently viewed products ... Faster."
 *
 * <p>Same behavior as the original: {@code set(key, value, ttlMs)} stores a value with an
 * absolute expiry timestamp (default TTL 30_000ms, matching the JS default parameter);
 * {@code get(key)} returns empty once the entry is missing OR past its expiry, lazily evicting
 * the expired entry on read (there is no background sweeper in the original either).
 */
public class InMemoryCache {

    private static final long DEFAULT_TTL_MS = 30_000L;

    private record Entry(Object value, long expiresAt) {
    }

    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    public void set(String key, Object value) {
        set(key, value, DEFAULT_TTL_MS);
    }

    public void set(String key, Object value, long ttlMs) {
        store.put(key, new Entry(value, System.currentTimeMillis() + ttlMs));
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        Entry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (System.currentTimeMillis() > entry.expiresAt()) {
            store.remove(key);
            return Optional.empty();
        }
        return Optional.of((T) entry.value());
    }

    public boolean has(String key) {
        return get(key).isPresent();
    }
}
