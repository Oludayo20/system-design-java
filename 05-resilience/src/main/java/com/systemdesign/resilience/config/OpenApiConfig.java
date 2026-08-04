package com.systemdesign.resilience.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI resilienceOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Resilience Demo")
                .description("Retries, circuit breakers, provider fallback, and queued payments")
                .version("1.0"));
    }
}
