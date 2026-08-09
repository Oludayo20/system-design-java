package com.systemdesign.freshcart.loyaltyconsumer.points;

import com.systemdesign.freshcart.loyaltyconsumer.rabbitmq.OrderPlacedEvent;
import com.systemdesign.freshcart.loyaltyconsumer.rabbitmq.RabbitMqConstants;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * The whole "day 2" integration is this class plus {@code RabbitMqConfig}'s queue binding.
 * Compare this class to {@code StockConsumer}/{@code NotificationsConsumer}/{@code StatsConsumer}
 * in the sibling apps — same shape, same pattern, zero coordination required with order-api or
 * with each other.
 */
@Component
public class PointsConsumer {

    private final PointsService pointsService;

    public PointsConsumer(PointsService pointsService) {
        this.pointsService = pointsService;
    }

    @RabbitListener(queues = RabbitMqConstants.LOYALTY_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        pointsService.awardForOrder(event);
    }
}
