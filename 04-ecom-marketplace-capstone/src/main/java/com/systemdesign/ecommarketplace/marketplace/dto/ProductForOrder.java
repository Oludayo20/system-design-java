package com.systemdesign.ecommarketplace.marketplace.dto;

import java.util.UUID;

/**
 * Mirrors the ProductForOrder interface in marketplace.service.ts. Narrow,
 * read-only shape for the Order module to validate a purchase against -
 * deliberately returns only what Order needs (id/name/price/stock), not the
 * full Product entity. OrdersService must never reach past this method into
 * Marketplace's table directly (module-boundary rule: modules talk through
 * public methods or events, not by poking each other's tables).
 */
public record ProductForOrder(UUID id, String name, int priceCents, int stock) {}
