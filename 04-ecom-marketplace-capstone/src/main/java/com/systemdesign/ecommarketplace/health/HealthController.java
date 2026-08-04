package com.systemdesign.ecommarketplace.health;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors src/modules/health/health.controller.ts. Public - polled by Nginx/compose healthchecks. */
@Tag(name = "health")
@RestController
public class HealthController {

  private final HealthService healthService;
  private final String instanceId;

  public HealthController(HealthService healthService, @Value("${app.instance-id}") String instanceId) {
    this.healthService = healthService;
    this.instanceId = instanceId;
  }

  @GetMapping("/health")
  public ResponseEntity<HealthReport> check() {
    HealthReport report = healthService.check(instanceId);
    HttpStatus status = "ok".equals(report.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return ResponseEntity.status(status).body(report);
  }
}
