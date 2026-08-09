package com.systemdesign.bookhive.notification.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI notificationServiceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("BookHive Notification Service")
                .description("""
                        Deliberately no database. This service's entire job in the lesson is to
                        demonstrate that a service can be simple (in-memory state, no persistence layer at
                        all) as long as nothing else in the system depends on it staying up -
                        order-service's fire-and-forget call (with a short timeout + swallowed errors) is
                        what makes that true.""")
                .version("1.0"));
    }
}
