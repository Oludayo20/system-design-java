package com.systemdesign.ecommarketplace.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Mirrors CreateOrderDto in src/modules/orders/dto/create-order.dto.ts. */
public record CreateOrderRequest(@NotEmpty @Valid List<CreateOrderItemRequest> items) {}
