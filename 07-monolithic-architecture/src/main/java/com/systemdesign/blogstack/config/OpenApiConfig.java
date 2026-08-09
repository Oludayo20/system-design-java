package com.systemdesign.blogstack.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI blogstackOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BlogStack API")
                        .description("""
                                Plain monolith reference: Auth, Users, Posts, Comments, and Notifications all in one \
                                Spring Boot application, one process, one shared PostgreSQL database. Modules call \
                                each other's services directly, in-process -- no event bus, no enforced boundaries.

                                **Auth flow:** POST /auth/register or /auth/login → copy accessToken → click \
                                **Authorize** and paste the token for the protected routes.""")
                        .version("1.0"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                                .name(BEARER_AUTH)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
