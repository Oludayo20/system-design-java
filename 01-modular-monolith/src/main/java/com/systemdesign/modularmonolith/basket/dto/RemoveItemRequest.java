package com.systemdesign.modularmonolith.basket.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Mirrors {@code remove-item.dto.ts}. Kept for parity with the source even though, as in the
 * original, it's currently unused -- {@code BasketController#removeItem} takes the product id
 * from the URL path instead of a request body.
 */
public record RemoveItemRequest(
        @NotNull UUID productId
) {
}
