package com.systemdesign.ecommarketplace.wallet.listener;

import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.OrderCreatedEvent;
import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants;
import com.systemdesign.ecommarketplace.wallet.WalletService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors src/modules/wallet/listeners/order-settlement.listener.ts. One of
 * four independent consumers of order.created (alongside Email, Inventory,
 * Analytics). This is the concrete "everything wired together" moment the
 * whole capstone is building toward: an event published by the Order module
 * (primary DB) is consumed here and resolved, via ShardRouterService
 * (inside WalletService), to a debit on the SHARD database that owns this
 * specific user - all inside the same Spring Boot process, running in
 * parallel with the other three workers.
 *
 * <p>The queue is declared (durable, with x-dead-letter-exchange) by
 * RabbitMQConfig; this listener just subscribes to it by name.
 */
@Component
public class OrderSettlementListener {

  private static final Logger log = LoggerFactory.getLogger(OrderSettlementListener.class);

  private final WalletService walletService;

  public OrderSettlementListener(WalletService walletService) {
    this.walletService = walletService;
  }

  @RabbitListener(queues = RabbitMQConstants.QUEUE_WALLET_SETTLEMENT_ON_ORDER_CREATED)
  public void handleOrderCreated(OrderCreatedEvent event) {
    log.info("[wallet-worker] Settling order {} for user {}", event.orderId(), event.userId());
    walletService.debit(
        event.userId(),
        event.totalCents(),
        "Order settlement for order " + event.orderId(),
        event.orderId());
    log.info("[wallet-worker] Debited {} cents from user {}'s wallet", event.totalCents(), event.userId());
  }
}
