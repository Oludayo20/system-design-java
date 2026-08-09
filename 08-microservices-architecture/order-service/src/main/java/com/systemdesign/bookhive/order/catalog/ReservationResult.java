package com.systemdesign.bookhive.order.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ReservationResult(UUID bookId, int unitPriceCents, int totalCents, int remainingStock) {
}
