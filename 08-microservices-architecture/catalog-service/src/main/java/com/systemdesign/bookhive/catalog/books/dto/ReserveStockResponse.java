package com.systemdesign.bookhive.catalog.books.dto;

import java.util.UUID;

public record ReserveStockResponse(UUID bookId, int unitPriceCents, int totalCents, int remainingStock) {
}
