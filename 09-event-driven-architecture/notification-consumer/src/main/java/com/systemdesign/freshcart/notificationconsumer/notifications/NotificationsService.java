package com.systemdesign.freshcart.notificationconsumer.notifications;

import com.systemdesign.freshcart.notificationconsumer.rabbitmq.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory store — fine for a teaching demo. A real notification service would persist sends
 * and integrate a push provider (FCM/APNs); the point here is that this consumer reacts to
 * order.placed on its own, with its own storage, on its own schedule.
 */
@Slf4j
@Service
public class NotificationsService {

    private final List<Notification> notifications = new CopyOnWriteArrayList<>();

    public void sendForOrder(OrderPlacedEvent event) {
        double total = event.payload().totalAmount().doubleValue();
        Notification notification = new Notification(
                UUID.randomUUID(),
                event.payload().orderId(),
                event.payload().customerId(),
                "Your FreshCart order (%d item(s), $%.2f) has been placed!"
                        .formatted(event.payload().items().size(), total),
                Instant.now());
        notifications.add(0, notification);
        log.info("order.placed (orderId={}, eventId={}): push sent to customer {}",
                event.payload().orderId(), event.eventId(), event.payload().customerId());
    }

    public List<Notification> getAll() {
        return notifications;
    }
}
