package com.systemdesign.ecommarketplace.orders;

import com.systemdesign.ecommarketplace.common.CurrentUser;
import com.systemdesign.ecommarketplace.common.JwtPayload;
import com.systemdesign.ecommarketplace.orders.dto.CreateOrderRequest;
import com.systemdesign.ecommarketplace.orders.dto.CreateOrderResult;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors src/modules/orders/orders.controller.ts. Protected - requires a valid bearer JWT. */
@Tag(name = "orders")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/orders")
public class OrdersController {

  private final OrdersService ordersService;

  public OrdersController(OrdersService ordersService) {
    this.ordersService = ordersService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CreateOrderResult create(@CurrentUser JwtPayload user, @Valid @RequestBody CreateOrderRequest dto) {
    return ordersService.createOrder(user.sub(), dto);
  }
}
