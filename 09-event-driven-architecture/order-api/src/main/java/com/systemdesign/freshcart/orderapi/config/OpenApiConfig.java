package com.systemdesign.freshcart.orderapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderApiOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FreshCart order-api — Event-Driven Architecture Reference")
                .description(
                        "The only HTTP producer in this project. POST /orders persists an order, "
                                + "commits the transaction, publishes order.placed to the "
                                + "grocery_events topic exchange, and returns — all in well under "
                                + "a second. It has zero code referencing inventory-consumer, "
                                + "notification-consumer, analytics-consumer, or loyalty-consumer; "
                                + "each of those is a separate app (own process, own port, own "
                                + "Maven module) that independently subscribes to the event. See "
                                + "the top-level README for the fan-out diagram and the "
                                + "idempotency/ordering demos.")
                .version("1.0"));
    }
}
