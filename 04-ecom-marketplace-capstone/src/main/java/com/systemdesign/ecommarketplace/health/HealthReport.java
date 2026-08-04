package com.systemdesign.ecommarketplace.health;

import java.util.Map;

/** Mirrors the HealthReport interface in src/modules/health/health.service.ts. */
public record HealthReport(String status, String instanceId, Map<String, String> services) {}
