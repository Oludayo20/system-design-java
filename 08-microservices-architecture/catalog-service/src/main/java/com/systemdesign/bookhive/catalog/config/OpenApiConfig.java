package com.systemdesign.bookhive.catalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI catalogServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BookHive Catalog Service")
                        .description("""
                                Owns catalog-db exclusively - no other BookHive service has its connection string.

                                Endpoints:
                                - POST /books - add a book (auth required)
                                - GET /books - list books
                                - GET /books/{id} - get one book
                                - POST /books/{id}/reserve - atomically decrement stock (called by order-service
                                  over HTTP, not a direct DB write)""")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
