package com.systemdesign.freshcart.analyticsconsumer.stats;

import com.systemdesign.freshcart.analyticsconsumer.rabbitmq.OrderPlacedEvent;
import com.systemdesign.freshcart.analyticsconsumer.rabbitmq.RabbitMqConstants;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/** This listener is the entire integration with order-api. No shared code, no imports. */
@Component
public class StatsConsumer {

    private final StatsService statsService;

    public StatsConsumer(StatsService statsService) {
        this.statsService = statsService;
    }

    @RabbitListener(queues = RabbitMqConstants.ANALYTICS_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        statsService.recordOrder(event);
    }
}
