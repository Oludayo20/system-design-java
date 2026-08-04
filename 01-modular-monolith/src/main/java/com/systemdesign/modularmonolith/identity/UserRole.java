package com.systemdesign.modularmonolith.identity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Mirrors the {@code UserRole} enum in {@code user.entity.ts}. Wire/DB representation is the
 * lowercase value ({@code "customer"} / {@code "admin"}), matching the Postgres
 * {@code identity.users.roles} text column and the JSON the NestJS API returns.
 */
public enum UserRole {
    CUSTOMER("customer"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static UserRole fromValue(String value) {
        for (UserRole role : values()) {
            if (role.value.equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }
}
