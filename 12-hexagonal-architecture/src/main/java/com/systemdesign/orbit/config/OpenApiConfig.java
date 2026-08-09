package com.systemdesign.orbit.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orbitOpenApi(
            @Value("${app.repository:memory}") String repository,
            @Value("${app.payment-provider:stripe}") String paymentProvider) {
        return new OpenAPI().info(new Info()
                .title("Orbit — Hexagonal Architecture")
                .description(
                        "Subscription billing core (ports & adapters). Active adapters: "
                                + "REPOSITORY=" + repository + ", PAYMENT_PROVIDER=" + paymentProvider + ".")
                .version("1.0"));
    }
}
