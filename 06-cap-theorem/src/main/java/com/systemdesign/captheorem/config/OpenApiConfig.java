package com.systemdesign.captheorem.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI capTheoremOpenApi() {
        return new OpenAPI().info(new Info()
                .title("CAP Theorem Demo")
                .description("AP product views vs CP wallet debits with a partition toggle")
                .version("1.0"));
    }
}
