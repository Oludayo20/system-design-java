package com.systemdesign.resilience.payment;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payment result from the winning provider.")
public record PaymentResult(
        @Schema(example = "paystack", allowableValues = {"paystack", "flutterwave", "cached-fallback"})
        String provider,
        @Schema(example = "paystack-1-5000") String reference,
        @Schema(example = "success", allowableValues = {"success", "queued"}) String status) {}
