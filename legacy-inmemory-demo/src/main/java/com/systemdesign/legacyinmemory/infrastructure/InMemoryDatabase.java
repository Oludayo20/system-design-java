package com.systemdesign.legacyinmemory.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * Java port of {@code infrastructure/database.js}'s {@code Database} class - a tiny in-memory
 * stand-in for PostgreSQL. doc.md: "Every module stores its data ... they're all using
 * PostgreSQL." Each module gets its own "table" (by name) but they all live in one logical
 * database instance, exactly like the original.
 *
 * <p>The original stores plain untyped JS objects in a {@code Map<string, object[]>} and
 * lets callers pass an arbitrary predicate. This port keeps that same untyped-storage shape
 * (a table is just {@code List<Object>}) but exposes generic {@code <T>} accessor methods so
 * each module can work with its own domain type without unchecked casts scattered everywhere -
 * the module code below only ever puts one Java type into a given table, so this is safe in
 * practice, just like the JS modules only ever put one "shape" of object into a given table.
 *
 * <p>{@code insert}/{@code find} simulate a network round-trip via {@link Delay#sleep(long)}
 * (20ms), exactly like the original's {@code await delay(20)}. {@code findAll} is synchronous
 * in both the original and here (used only for in-process reads such as
 * {@code catalog.listProducts()} and the sharding demo's distribution summary).
 */
public class InMemoryDatabase {

    private final String label;
    private final Map<String, List<Object>> tables = new ConcurrentHashMap<>();

    public InMemoryDatabase() {
        this("postgres");
    }

    public InMemoryDatabase(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public List<Object> table(String name) {
        return tables.computeIfAbsent(name, k -> new CopyOnWriteArrayList<>());
    }

    public <T> T insert(String tableName, T record) {
        Delay.sleep(20); // simulate a write round-trip
        table(tableName).add(record);
        return record;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> find(String tableName, Predicate<T> predicate) {
        Delay.sleep(20); // simulate a read round-trip
        for (Object record : table(tableName)) {
            T typed = (T) record;
            if (predicate.test(typed)) {
                return Optional.of(typed);
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(String tableName, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (Object record : table(tableName)) {
            T typed = (T) record;
            if (predicate.test(typed)) {
                result.add(typed);
            }
        }
        return result;
    }

    public <T> List<T> findAll(String tableName) {
        return findAll(tableName, t -> true);
    }
}
