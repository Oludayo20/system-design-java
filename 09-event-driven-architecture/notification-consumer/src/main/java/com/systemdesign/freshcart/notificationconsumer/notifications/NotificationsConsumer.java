package com.systemdesign.freshcart.notificationconsumer.notifications;

import com.systemdesign.freshcart.notificationconsumer.rabbitmq.OrderPlacedEvent;
import com.systemdesign.freshcart.notificationconsumer.rabbitmq.RabbitMqConstants;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** This listener is the entire integration with order-api. No shared code, no imports. */
@Component
public class NotificationsConsumer {

    private final NotificationsService notificationsService;

    public NotificationsConsumer(NotificationsService notificationsService) {
        this.notificationsService = notificationsService;
    }

    @RabbitListener(queues = RabbitMqConstants.NOTIFICATION_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        notificationsService.sendForOrder(event);
    }
}
