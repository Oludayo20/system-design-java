package com.systemdesign.ecommarketplace.workers;

import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.OrderCreatedEvent;
import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants;
import com.systemdesign.ecommarketplace.marketplace.MarketplaceService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors src/modules/workers/inventory.worker.ts. Reduces stock via
 * MarketplaceService.decrementStock - the same narrow,
 * module-boundary-respecting method OrdersService would use if it ever
 * needed to touch stock (it doesn't; that's this worker's job). Nothing
 * touches ProductRepository directly outside the Marketplace module.
 */
@Component
public class InventoryWorker {

  private static final Logger log = LoggerFactory.getLogger(InventoryWorker.class);

  private final MarketplaceService marketplaceService;

  public InventoryWorker(MarketplaceService marketplaceService) {
    this.marketplaceService = marketplaceService;
  }

  @RabbitListener(queues = RabbitMQConstants.QUEUE_INVENTORY_ON_ORDER_CREATED)
  public void handleOrderCreated(OrderCreatedEvent event) {
    for (OrderCreatedEvent.Item item : event.items()) {
      log.info(
          "[inventory-worker] Order {}: decrementing stock for product {} by {}",
          event.orderId(),
          item.productId(),
          item.quantity());
      marketplaceService.decrementStock(UUID.fromString(item.productId()), item.quantity());
    }
  }
}
