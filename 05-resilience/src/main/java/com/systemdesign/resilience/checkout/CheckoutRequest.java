package com.systemdesign.resilience.checkout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CheckoutRequest(@NotNull @Positive Double amount) {}
