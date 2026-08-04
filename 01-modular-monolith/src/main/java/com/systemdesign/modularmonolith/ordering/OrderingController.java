package com.systemdesign.modularmonolith.ordering;

import com.systemdesign.modularmonolith.identity.AuthenticatedUser;
import com.systemdesign.modularmonolith.ordering.dto.PlaceOrderResult;
import com.systemdesign.modularmonolith.ordering.entity.Order;
import com.systemdesign.modularmonolith.shared.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Mirrors {@code src/modules/ordering/ordering.controller.ts}. Requires a valid bearer token for
 * every route (see {@code identity.security.SecurityConfig}).
 */
@RestController
@RequestMapping("/orders")
public class OrderingController {

    private final OrderingService orderingService;

    public OrderingController(OrderingService orderingService) {
        this.orderingService = orderingService;
    }

    @PostMapping
    public PlaceOrderResult placeOrder(@CurrentUser AuthenticatedUser user) {
        return orderingService.placeOrder(user.userId());
    }

    @GetMapping
    public List<Order> listOrders(@CurrentUser AuthenticatedUser user) {
        return orderingService.listOrders(user.userId());
    }

    @GetMapping("/{id}")
    public Order getOrder(@CurrentUser AuthenticatedUser user, @PathVariable UUID id) {
        return orderingService.getOrder(user.userId(), id);
    }

    @PostMapping("/{id}/cancel")
    public Order cancelOrder(@CurrentUser AuthenticatedUser user, @PathVariable UUID id) {
        return orderingService.cancelOrder(user.userId(), id);
    }
}
