package com.systemdesign.faas.store;

import java.time.Instant;
import java.util.List;

public record Order(String id, String customerId, List<OrderItem> items, long total, Instant createdAt) {
}
