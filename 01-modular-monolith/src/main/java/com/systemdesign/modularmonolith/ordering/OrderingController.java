package com.systemdesign.modularmonolith.ordering;

import com.systemdesign.modularmonolith.identity.AuthenticatedUser;
import com.systemdesign.modularmonolith.ordering.dto.PlaceOrderResult;
import com.systemdesign.modularmonolith.ordering.entity.Order;
import com.systemdesign.modularmonolith.shared.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "ordering", description = "Place, list, view, and cancel orders — requires Bearer token")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/orders")
public class OrderingController {

    private final OrderingService orderingService;

    public OrderingController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    @PostMapping
    @Operation(
            summary = "Place an order from the current basket",
            description = "Commits the order, publishes order.created to RabbitMQ without awaiting consumers, and returns immediately.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PlaceOrderResult.class)))
    @ApiResponse(responseCode = "403", description = "Basket is empty.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
    public PlaceOrderResult placeOrder(@CurrentUser AuthenticatedUser user) {
        return orderingService.placeOrder(user.userId());
    }

    @GetMapping
    @Operation(summary = "List order history for the current user")
    @ApiResponse(responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Order.class))))
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
    public List<Order> listOrders(@CurrentUser AuthenticatedUser user) {
        return orderingService.listOrders(user.userId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single order by ID")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Order.class)))
    @ApiResponse(responseCode = "404", description = "Order not found or not owned by this user.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
    public Order getOrder(
            @CurrentUser AuthenticatedUser user,
            @Parameter(description = "Order UUID") @PathVariable UUID id) {
        return orderingService.getOrder(user.userId(), id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a placed order", description = "Only orders in placed status can be cancelled.")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = Order.class)))
    @ApiResponse(responseCode = "404", description = "Order not found.")
    @ApiResponse(responseCode = "403", description = "Order cannot be cancelled.")
    @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
    public Order cancelOrder(
            @CurrentUser AuthenticatedUser user,
            @Parameter(description = "Order UUID") @PathVariable UUID id) {
        return orderingService.cancelOrder(user.userId(), id);
    }
}
