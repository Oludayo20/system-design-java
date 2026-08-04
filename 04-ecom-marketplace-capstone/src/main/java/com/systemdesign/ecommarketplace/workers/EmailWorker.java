package com.systemdesign.ecommarketplace.workers;

import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.OrderCreatedEvent;
import com.systemdesign.ecommarketplace.infrastructure.rabbitmq.RabbitMQConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Mirrors src/modules/workers/email.worker.ts. Simulates doc.md's "Send
 * Receipt" step - the slow task the whole point of this architecture is to
 * keep off the request/response path. In production this would call a real
 * email provider (SES, Postmark, etc); here it logs, which is enough to
 * observe in `docker compose logs -f` that it ran independently of the API
 * response.
 */
@Component
public class EmailWorker {

  private static final Logger log = LoggerFactory.getLogger(EmailWorker.class);

  @RabbitListener(queues = RabbitMQConstants.QUEUE_EMAIL_ON_ORDER_CREATED)
  public void handleOrderCreated(OrderCreatedEvent event) {
    log.info(
        "[email-worker] Sending receipt for order {} to user {} (total: {} cents)",
        event.orderId(),
        event.userId(),
        event.totalCents());
  }
}
