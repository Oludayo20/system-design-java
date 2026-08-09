package com.systemdesign.bookhive.order.orders.dto;

import com.systemdesign.bookhive.order.orders.entity.Order;

import java.util.UUID;

public record OrderResponse(UUID id, UUID userId, UUID bookId, Integer quantity, Integer unitPriceCents,
                             Integer totalCents, String status) {

    public static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getUserId(), order.getBookId(), order.getQuantity(),
                order.getUnitPriceCents(), order.getTotalCents(), order.getStatus());
    }
}
