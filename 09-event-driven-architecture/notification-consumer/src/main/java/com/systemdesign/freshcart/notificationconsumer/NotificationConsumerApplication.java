package com.systemdesign.freshcart.notificationconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * One of four independent consumers of {@code order.placed}. This is the entire integration
 * surface with order-api: bind a queue to the {@code grocery_events} exchange.
 */
@SpringBootApplication
public class NotificationConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationConsumerApplication.class, args);
    }
}
