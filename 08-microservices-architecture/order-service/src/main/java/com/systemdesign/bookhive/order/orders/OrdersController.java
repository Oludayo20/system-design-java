package com.systemdesign.bookhive.order.orders;

import com.systemdesign.bookhive.order.common.JwtPayload;
import com.systemdesign.bookhive.order.orders.dto.CreateOrderRequest;
import com.systemdesign.bookhive.order.orders.dto.OrderResponse;
import com.systemdesign.bookhive.order.security.JwtVerifier;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "orders")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/orders")
public class OrdersController {

    private final OrdersService ordersService;
    private final JwtVerifier jwtVerifier;

    public OrdersController(OrdersService ordersService, JwtVerifier jwtVerifier) {
        this.ordersService = ordersService;
        this.jwtVerifier = jwtVerifier;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Place an order",
            description = "userId comes from the JWT `sub` claim, not the request body. Calls "
                    + "catalog-service over HTTP to check price/stock and decrement it, then "
                    + "fire-and-forgets a call to notification-service (see README \"Fault isolation\" - "
                    + "stop notification-service and this endpoint still succeeds).")
    public OrderResponse placeOrder(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @Valid @RequestBody CreateOrderRequest dto) {
        JwtPayload user = jwtVerifier.requireBearer(authorization);
        return OrderResponse.from(ordersService.placeOrder(UUID.fromString(user.sub()), dto, authorization));
    }

    @GetMapping
    @Operation(summary = "List the current user's orders")
    public List<OrderResponse> findAll(@RequestHeader(value = "Authorization", required = false) String authorization) {
        JwtPayload user = jwtVerifier.requireBearer(authorization);
        return ordersService.findAllForUser(UUID.fromString(user.sub())).stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one of the current user's orders")
    public OrderResponse findOne(@RequestHeader(value = "Authorization", required = false) String authorization,
                                  @PathVariable UUID id) {
        JwtPayload user = jwtVerifier.requireBearer(authorization);
        return OrderResponse.from(ordersService.findOneForUser(id, UUID.fromString(user.sub())));
    }
}
