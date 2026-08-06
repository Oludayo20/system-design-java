package com.systemdesign.modularmonolith.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI eshopOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("E-Shop API")
                        .description("""
                                Modular monolith reference (Spring Boot): Identity, Catalog, Basket, and Ordering \
                                modules with strict boundaries. Domain events flow through RabbitMQ; Inventory and \
                                Notifications are async consumers. PostgreSQL uses schema-per-module; Redis provides \
                                cache-aside and sessions.

                                **Auth flow:** POST /auth/register or /auth/login → copy accessToken → click \
                                **Authorize** for Basket and Ordering routes.""")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
