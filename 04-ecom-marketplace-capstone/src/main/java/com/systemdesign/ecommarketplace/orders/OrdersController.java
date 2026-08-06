package com.systemdesign.ecommarketplace.orders;

import com.systemdesign.ecommarketplace.common.CurrentUser;
import com.systemdesign.ecommarketplace.common.JwtPayload;
import com.systemdesign.ecommarketplace.orders.dto.CreateOrderRequest;
import com.systemdesign.ecommarketplace.orders.dto.CreateOrderResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "orders", description = "Place orders — requires JWT")
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
  @Operation(
      summary = "Place an order",
      description = "Validates stock, persists on primary DB, publishes order.created, returns immediately. Workers settle asynchronously.")
  @ApiResponse(responseCode = "201", content = @Content(schema = @Schema(implementation = CreateOrderResult.class)))
  @ApiResponse(responseCode = "400", description = "Insufficient stock or invalid items.")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT.")
  public CreateOrderResult create(@CurrentUser JwtPayload user, @Valid @RequestBody CreateOrderRequest dto) {
    return ordersService.createOrder(user.sub(), dto);
  }
}
