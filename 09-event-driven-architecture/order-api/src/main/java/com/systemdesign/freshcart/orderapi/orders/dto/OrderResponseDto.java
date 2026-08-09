package com.systemdesign.freshcart.orderapi.orders.dto;

import com.systemdesign.freshcart.orderapi.orders.Order;
import com.systemdesign.freshcart.orderapi.orders.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponseDto(

        @Schema(example = "5f1b6b2e-8c4d-4a1e-9f3b-2d7e6a5b4c3d")
        UUID id,

        @Schema(example = "customer-42")
        String customerId,

        List<OrderItemDto> items,

        @Schema(example = "9.00")
        BigDecimal totalAmount,

        @Schema(example = "placed")
        String status,

        @Schema(example = "2026-08-08T12:00:00Z")
        Instant createdAt) {

    public static OrderResponseDto from(Order order) {
        List<OrderItemDto> items = order.getItems().stream()
                .map(item -> new OrderItemDto(item.getSku(), item.getName(), item.getQuantity(), item.getUnitPrice()))
                .toList();
        return new OrderResponseDto(
                order.getId(), order.getCustomerId(), items, order.getTotalAmount(), order.getStatus(),
                order.getCreatedAt());
    }
}
