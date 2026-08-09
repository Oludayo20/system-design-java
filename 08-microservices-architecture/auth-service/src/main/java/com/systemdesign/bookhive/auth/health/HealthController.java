package com.systemdesign.bookhive.auth.health;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** A plain liveness probe for docker-compose healthchecks. Excluded from Swagger. */
@Hidden
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> check() {
        return Map.of("status", "ok", "service", "auth-service");
    }
}
