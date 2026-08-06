package com.systemdesign.ecommarketplace.health;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Infrastructure health report from GET /health.")
public record HealthReport(
    @Schema(example = "ok", allowableValues = {"ok", "degraded"}) String status,
    @Schema(example = "api-1") String instanceId,
    @Schema(description = "Per-dependency probe results.") Map<String, String> services) {}
