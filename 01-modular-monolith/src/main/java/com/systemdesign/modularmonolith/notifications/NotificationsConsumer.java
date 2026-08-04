package com.systemdesign.modularmonolith.notifications;

import com.systemdesign.modularmonolith.infrastructure.rabbitmq.EventEnvelope;
import com.systemdesign.modularmonolith.infrastructure.rabbitmq.RabbitMqConstants;
import com.systemdesign.modularmonolith.ordering.dto.OrderCreatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * The Notifications "module": simulates the slow part of the request (sending an email receipt)
 * entirely outside the HTTP request/response cycle. {@code POST /orders} already returned
 * {@code {"success": true}} to the customer well before this handler even starts running.
 *
 * Mirrors {@code src/modules/notifications/notifications.consumer.ts}.
 */
@Slf4j
@Component
public class NotificationsConsumer {

    private static final long SIMULATED_EMAIL_SEND_MS = 5_000;

    @RabbitListener(queues = RabbitMqConstants.NOTIFICATIONS_ORDER_CREATED_QUEUE)
    public void handleOrderCreated(EventEnvelope<OrderCreatedEvent> envelope) throws InterruptedException {
        OrderCreatedEvent payload = envelope.payload();
        log.info("Sending receipt email for order {} to user {} (simulated, ~5s)...",
                payload.orderId(), payload.userId());

        Thread.sleep(SIMULATED_EMAIL_SEND_MS);

        log.info("Receipt email sent for order {}, total ${}", payload.orderId(), payload.total());
    }
}
