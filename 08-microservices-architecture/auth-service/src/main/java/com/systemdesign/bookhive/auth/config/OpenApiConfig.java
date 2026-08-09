package com.systemdesign.bookhive.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authServiceOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BookHive Auth Service")
                        .description("""
                                Owns auth-db exclusively - no other BookHive service has its connection string.

                                Endpoints:
                                - POST /auth/register - create a user, receive a JWT
                                - POST /auth/login - receive a JWT
                                - GET /auth/verify - verify a bearer token by hand

                                catalog-service and order-service verify JWTs in-process against the same
                                JWT_SECRET - they never call this service at request time.""")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
