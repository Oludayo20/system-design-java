package com.systemdesign.freshcart.inventoryconsumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * One of four independent consumers of {@code order.placed}. This is the entire integration
 * surface with order-api: bind a queue to the {@code grocery_events} exchange. There is no
 * import of, call to, or awareness of order-api's code anywhere in this Maven module.
 */
@SpringBootApplication
public class InventoryConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryConsumerApplication.class, args);
    }
}
