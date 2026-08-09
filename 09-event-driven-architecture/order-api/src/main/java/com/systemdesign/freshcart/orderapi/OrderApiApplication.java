package com.systemdesign.freshcart.orderapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The only HTTP producer in FreshCart. See {@code orders.OrderService#placeOrder} for the
 * publish-after-commit mechanics, and the top-level README for the fan-out diagram.
 */
@SpringBootApplication
public class OrderApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderApiApplication.class, args);
    }
}
