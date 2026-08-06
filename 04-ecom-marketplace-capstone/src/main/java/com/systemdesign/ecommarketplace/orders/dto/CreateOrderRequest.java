package com.systemdesign.ecommarketplace.orders.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "Place order with one or more line items.")
public record CreateOrderRequest(@NotEmpty @Valid List<CreateOrderItemRequest> items) {}
