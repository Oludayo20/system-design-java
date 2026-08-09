package com.systemdesign.freshcart.orderapi.orders;

import com.systemdesign.freshcart.orderapi.orders.dto.CreateOrderDto;
import com.systemdesign.freshcart.orderapi.orders.dto.OrderResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "orders", description = "Order placement — the sole producer of order.placed")
@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Place an order (the only producer endpoint in this project)",
            description = "Synchronous path: persists the order to PostgreSQL inside a "
                    + "transaction (~ms). Async path: once — and only once — that transaction "
                    + "has committed, publishes order.placed to the grocery_events topic "
                    + "exchange, then returns immediately. inventory-consumer, "
                    + "notification-consumer, analytics-consumer, and loyalty-consumer each "
                    + "independently subscribe to this event in separate processes; order-api "
                    + "never calls them, never waits for them, and does not know they exist.")
    @ApiResponse(responseCode = "201", description = "Order committed and order.placed published. Consumers react asynchronously.",
            content = @Content(schema = @Schema(implementation = OrderResponseDto.class)))
    @ApiResponse(responseCode = "400", description = "Validation failed.")
    public OrderResponseDto placeOrder(@Valid @RequestBody CreateOrderDto dto) {
        return orderService.placeOrder(dto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Fetch a previously placed order")
    @ApiResponse(responseCode = "200", description = "Order found.",
            content = @Content(schema = @Schema(implementation = OrderResponseDto.class)))
    @ApiResponse(responseCode = "404", description = "Order not found.")
    public OrderResponseDto getOrder(@Parameter(description = "Order UUID") @PathVariable UUID id) {
        return orderService.getOrder(id);
    }
}
