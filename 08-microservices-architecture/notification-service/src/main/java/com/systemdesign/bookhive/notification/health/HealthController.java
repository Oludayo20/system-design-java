package com.systemdesign.bookhive.notification.health;

import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Hidden
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> check() {
        return Map.of("status", "ok", "service", "notification-service");
    }
}
