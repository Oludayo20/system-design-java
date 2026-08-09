package com.systemdesign.faas.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI serverlessOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Serverless Architecture — Local FaaS Emulator")
                .description("Hand-built local emulator: real cold starts, scale-to-zero, per-invocation billing, and concurrency scaling")
                .version("1.0"));
    }
}
