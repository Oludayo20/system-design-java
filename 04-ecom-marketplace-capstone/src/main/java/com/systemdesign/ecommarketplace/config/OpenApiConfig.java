package com.systemdesign.ecommarketplace.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Mirrors main.ts's DocumentBuilder().setTitle(...).setDescription(...)
 * .setVersion(...).addBearerAuth().build() + SwaggerModule.setup('docs', ...).
 * Defines the "bearerAuth" security scheme that Users/Orders/Wallet
 * controllers reference via @SecurityRequirement(name = "bearerAuth"), so
 * Swagger UI's Authorize button actually works. springdoc serves this at
 * /v3/api-docs and renders it at /docs (see application.yml's
 * springdoc.swagger-ui.path).
 */
@Configuration
public class OpenApiConfig {

  private static final String BEARER_AUTH_SCHEME = "bearerAuth";

  @Bean
  public OpenAPI ojaMarketplaceOpenApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Oja Marketplace Capstone API")
                .description(
                    "Modular monolith + sharded Postgres (Users/Wallet) + RabbitMQ async workers,"
                        + " behind Nginx with 2 API replicas.")
                .version("1.0"))
        .components(
            new Components()
                .addSecuritySchemes(
                    BEARER_AUTH_SCHEME,
                    new SecurityScheme()
                        .name(BEARER_AUTH_SCHEME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
