package com.systemdesign.ecommarketplace.infrastructure.rabbitmq;

/**
 * Mirrors src/infrastructure/rabbitmq/constants.ts: a topic exchange named
 * `domain_events`, one routing key per domain event, one durable queue per
 * independent worker. Multiple queues bound to the same routing key is
 * exactly what makes 4 workers (Email, Inventory, Analytics, Wallet
 * settlement) all react to a single order.created publish, independently
 * and in parallel.
 */
public final class RabbitMQConstants {

  private RabbitMQConstants() {}

  public static final String DOMAIN_EVENTS_EXCHANGE = "domain_events";
  public static final String DOMAIN_EVENTS_DLX = "domain_events.dlx";

  public static final String ROUTING_KEY_ORDER_CREATED = "order.created";

  public static final String QUEUE_EMAIL_ON_ORDER_CREATED = "email.order_created";
  public static final String QUEUE_INVENTORY_ON_ORDER_CREATED = "inventory.order_created";
  public static final String QUEUE_ANALYTICS_ON_ORDER_CREATED = "analytics.order_created";
  public static final String QUEUE_WALLET_SETTLEMENT_ON_ORDER_CREATED = "wallet_settlement.order_created";
}
