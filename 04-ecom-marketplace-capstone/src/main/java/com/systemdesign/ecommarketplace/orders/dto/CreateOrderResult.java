package com.systemdesign.ecommarketplace.orders.dto;

/** Mirrors CreateOrderResult in src/modules/orders/orders.service.ts: { success: true, orderId }. */
public record CreateOrderResult(boolean success, String orderId) {}
