package com.systemdesign.captheorem.cap;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Wallet debit amount.")
public record DebitRequest(@Schema(example = "500") @NotNull @Positive Integer amount) {}
