package com.systemdesign.orbit.adapters.in.http.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SubscribeRequest(
        @Schema(example = "cust_123", description = "Opaque customer identifier")
        @NotBlank
        String customerId,

        @Schema(example = "pro", allowableValues = {"basic", "pro", "enterprise"})
        @NotBlank
        @Pattern(regexp = "basic|pro|enterprise", message = "planId must be one of: basic, pro, enterprise")
        String planId) {
}
