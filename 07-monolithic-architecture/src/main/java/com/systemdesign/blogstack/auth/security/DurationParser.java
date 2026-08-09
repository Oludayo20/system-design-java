package com.systemdesign.blogstack.auth.security;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal parser for the duration string formats {@code @nestjs/jwt} (via the {@code ms} npm
 * package) accepts for {@code JWT_EXPIRES_IN}, e.g. "1h", "30m", "7d", "3600s", or a bare number
 * of seconds. Covers the formats this project's .env.example actually uses.
 */
final class DurationParser {

    private static final Pattern PATTERN = Pattern.compile("^(\\d+)\\s*(ms|s|m|h|d)?$", Pattern.CASE_INSENSITIVE);

    private DurationParser() {
    }

    static Duration parse(String value) {
        if (value == null || value.isBlank()) {
            return Duration.ofHours(1);
        }
        Matcher matcher = PATTERN.matcher(value.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Unsupported JWT_EXPIRES_IN value: " + value);
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2) == null ? "s" : matcher.group(2).toLowerCase();
        return switch (unit) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new IllegalArgumentException("Unsupported JWT_EXPIRES_IN unit: " + unit);
        };
    }
}
