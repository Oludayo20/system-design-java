package com.systemdesign.library.books.presentation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateBookRequest(
        @Schema(example = "Clean Architecture") @NotBlank String title,
        @Schema(example = "Robert C. Martin") @NotBlank String author,
        @Schema(example = "9780134494166") @NotBlank String isbn,
        @Schema(example = "3", minimum = "1", description = "Copies the library owns.") @Min(1) int totalCopies) {
}
