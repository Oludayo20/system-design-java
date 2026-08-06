package com.systemdesign.resilience.checkout;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Checkout payment request.")
public record CheckoutRequest(
        @Schema(example = "5000", description = "Amount in minor currency units (kobo/cents).")
        @NotNull @Positive Double amount) {}
