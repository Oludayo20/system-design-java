package com.systemdesign.ecommarketplace.infrastructure.rabbitmq;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Mirrors the OrderCreatedEvent interface in
 * src/infrastructure/rabbitmq/constants.ts. Serialized to/from JSON on the
 * `domain_events` exchange by Jackson2JsonMessageConverter (see
 * RabbitMQConfig) - every consumer (Email/Inventory/Analytics/Wallet
 * workers) deserializes the exact same payload shape.
 */
public record OrderCreatedEvent(
    String orderId, String userId, int totalCents, List<Item> items, String createdAt) {

  @JsonCreator
  public OrderCreatedEvent(
      @JsonProperty("orderId") String orderId,
      @JsonProperty("userId") String userId,
      @JsonProperty("totalCents") int totalCents,
      @JsonProperty("items") List<Item> items,
      @JsonProperty("createdAt") String createdAt) {
    this.orderId = orderId;
    this.userId = userId;
    this.totalCents = totalCents;
    this.items = items;
    this.createdAt = createdAt;
  }

  public record Item(String productId, int quantity, int unitPriceCents) {

    @JsonCreator
    public Item(
        @JsonProperty("productId") String productId,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("unitPriceCents") int unitPriceCents) {
      this.productId = productId;
      this.quantity = quantity;
      this.unitPriceCents = unitPriceCents;
    }
  }
}
