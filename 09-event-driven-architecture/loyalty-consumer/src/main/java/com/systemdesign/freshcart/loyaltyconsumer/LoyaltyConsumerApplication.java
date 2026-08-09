package com.systemdesign.freshcart.loyaltyconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The "day 2" consumer — see the README section "Adding loyalty-consumer on day 2". Written and
 * wired up after order-api, inventory-consumer, notification-consumer, and analytics-consumer
 * already existed in production. Getting it live required a new app and a new queue binding; it
 * required zero changes to order-api or any sibling consumer.
 */
@SpringBootApplication
public class LoyaltyConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoyaltyConsumerApplication.class, args);
    }
}
