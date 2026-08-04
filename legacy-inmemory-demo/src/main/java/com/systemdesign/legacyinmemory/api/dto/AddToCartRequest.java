package com.systemdesign.legacyinmemory.api.dto;

/**
 * Request body for {@code POST /basket/:userId/items}. {@code qty} is nullable so the
 * controller can reproduce the original's {@code req.body.qty ?? 1} default (only a missing/
 * null qty defaults to 1 - an explicit {@code 0} is kept as-is, matching JS {@code ??}
 * semantics).
 */
public record AddToCartRequest(Integer productId, Integer qty) {
}
