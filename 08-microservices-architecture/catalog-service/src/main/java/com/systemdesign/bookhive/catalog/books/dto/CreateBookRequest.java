package com.systemdesign.bookhive.catalog.books.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookRequest(
        @Schema(example = "The Pragmatic Programmer")
        @NotBlank String title,

        @Schema(example = "David Thomas & Andrew Hunt")
        @NotBlank String author,

        @Schema(example = "3500", description = "Price in cents")
        @NotNull @Min(0) Integer priceCents,

        @Schema(example = "25", description = "Units in stock")
        @NotNull @Min(0) Integer stock) {
}
