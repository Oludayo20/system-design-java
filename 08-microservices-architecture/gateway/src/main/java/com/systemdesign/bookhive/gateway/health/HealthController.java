package com.systemdesign.bookhive.gateway.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Handled by an exact "/health" mapping, which Spring's path matching prefers over the
 * catch-all "/**" in {@link com.systemdesign.bookhive.gateway.proxy.ProxyController} - so this
 * never gets proxied anywhere, exactly like the source Express gateway's {@code app.get('/health', ...)}
 * registered ahead of its proxy middleware.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> check() {
        return Map.of("status", "ok", "service", "gateway");
    }
}
