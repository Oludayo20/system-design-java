package com.systemdesign.bookhive.order.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BookHive Order Service")
                        .description("""
                                Owns order-db exclusively - has NO catalog-db or auth-db connection info
                                anywhere in its environment. Reaches catalog-service only over HTTP.

                                Endpoints:
                                - POST /orders - place an order (auth required)
                                - GET /orders - list your orders
                                - GET /orders/{id} - get one of your orders

                                Placing an order calls catalog-service synchronously (must succeed) and
                                notification-service fire-and-forget (may fail silently - fault isolation).""")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
