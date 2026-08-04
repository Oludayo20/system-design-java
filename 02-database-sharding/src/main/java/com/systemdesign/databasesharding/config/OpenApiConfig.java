package com.systemdesign.databasesharding.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mirrors the DocumentBuilder() call in main.ts. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI databaseShardingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Database Sharding Demo")
                        .description(
                                "Reference implementation of a shard router over three independent PostgreSQL databases.")
                        .version("1.0"));
    }
}
