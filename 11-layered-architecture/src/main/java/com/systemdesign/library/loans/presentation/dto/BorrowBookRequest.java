package com.systemdesign.library.loans.presentation.dto;

import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record BorrowBookRequest(
        @Schema(example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890") @NotNull UUID bookId,
        @Schema(example = "b2c3d4e5-f6a7-8901-bcde-f12345678901") @NotNull UUID memberId) {
}
