package com.systemdesign.freshcart.inventoryconsumer.stock;

import com.systemdesign.freshcart.inventoryconsumer.rabbitmq.OrderPlacedEvent;
import com.systemdesign.freshcart.inventoryconsumer.rabbitmq.RabbitMqConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * This {@code @RabbitListener} is the entire integration surface with order-api. There is no
 * import of, call to, or awareness of order-api's OrderController/OrderService anywhere in this
 * Maven module. If FreshCart ships a fifth consumer tomorrow, it looks exactly like this class —
 * order-api's source code does not change by a single line (see loyalty-consumer for that proof
 * in action).
 */
@Slf4j
@Component
public class StockConsumer {

    private final StockService stockService;

    public StockConsumer(StockService stockService) {
        this.stockService = stockService;
    }

    @RabbitListener(queues = RabbitMqConstants.INVENTORY_QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        stockService.applyOrder(event);
    }
}
