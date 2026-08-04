package com.systemdesign.ecommarketplace.workers;

import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.OrderCreatedEvent;
import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** Mirrors src/modules/workers/analytics.worker.ts. Simulates recording a sale into an analytics pipeline/warehouse. */
@Component
public class AnalyticsWorker {

  private static final Logger log = LoggerFactory.getLogger(AnalyticsWorker.class);

  @RabbitListener(queues = RabbitMQConstants.QUEUE_ANALYTICS_ON_ORDER_CREATED)
  public void handleOrderCreated(OrderCreatedEvent event) {
    log.info(
        "[analytics-worker] Recorded sale: order {}, user {}, {} line item(s), total {} cents",
        event.orderId(),
        event.userId(),
        event.items().size(),
        event.totalCents());
  }
}
