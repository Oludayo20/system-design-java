package com.systemdesign.modularmonolith.ordering.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Mirrors the {@code OrderStatus} enum in {@code order.entity.ts}. Wire/DB representation is the
 * lowercase value ({@code "placed"} / {@code "cancelled"}), matching the Postgres native enum
 * type {@code ordering.ordering_order_status} created in V5 and the JSON the NestJS API returns.
 */
public enum OrderStatus {
    PLACED("placed"),
    CANCELLED("cancelled");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static OrderStatus fromValue(String value) {
        for (OrderStatus status : values()) {
            if (status.value.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown order status: " + value);
    }
}
