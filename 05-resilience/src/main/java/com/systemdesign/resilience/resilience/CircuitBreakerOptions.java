package com.systemdesign.resilience.resilience;

public record CircuitBreakerOptions(int failureThreshold, long resetTimeoutMs) {}
