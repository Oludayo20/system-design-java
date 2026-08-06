package com.systemdesign.ecommarketplace.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "health", description = "Load balancer / orchestration health probe")
@RestController
public class HealthController {

  private final HealthService healthService;
  private final String instanceId;

  public HealthController(HealthService healthService, @Value("${app.instance-id}") String instanceId) {
    this.healthService = healthService;
    this.instanceId = instanceId;
  }

  @GetMapping("/health")
  @Operation(
      summary = "Infrastructure health check",
      description = "Probes primary Postgres, all three shards, Redis, and RabbitMQ. Returns 503 if any dependency is down.")
  @ApiResponse(responseCode = "200", description = "All dependencies healthy.", content = @Content(schema = @Schema(implementation = HealthReport.class)))
  @ApiResponse(responseCode = "503", description = "One or more dependencies unreachable.", content = @Content(schema = @Schema(implementation = HealthReport.class)))
  public ResponseEntity<HealthReport> check() {
    HealthReport report = healthService.check(instanceId);
    HttpStatus status = "ok".equals(report.status()) ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return ResponseEntity.status(status).body(report);
  }
}
