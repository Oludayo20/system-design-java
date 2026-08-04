package com.systemdesign.asyncqueue.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * API metadata only — mirrors the {@code DocumentBuilder} call in src/main.ts. springdoc-openapi
 * itself auto-configures the {@code /docs} UI (see application.yml's {@code springdoc.swagger-ui.path})
 * whenever Spring MVC is present; this bean is inert (and harmless) in the worker process, which
 * has no web server for springdoc to attach to.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI asyncQueueOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Async Queue Processing — Reference API")
                .description("POST /rides persists a ride and publishes ride.completed to RabbitMQ, then returns "
                        + "immediately. Email, analytics, and loyalty workers process it in a separate process.")
                .version("1.0"));
    }
}
