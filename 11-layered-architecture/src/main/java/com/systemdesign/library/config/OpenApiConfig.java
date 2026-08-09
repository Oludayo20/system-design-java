package com.systemdesign.library.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI libraryOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Riverside Library API")
                .description(
                        "N-Tier / Layered Architecture reference: every request flows one direction only -- "
                                + "Presentation -> Application -> Domain -> Data Access -> Database. "
                                + "The Domain layer (business rules) is plain Java with zero framework imports.\n\n"
                                + "Try the borrowing rules: create a book with totalCopies=1, create a member, then "
                                + "POST /loans to borrow, and try to borrow again to see the domain reject it.")
                .version("1.0"));
    }
}
