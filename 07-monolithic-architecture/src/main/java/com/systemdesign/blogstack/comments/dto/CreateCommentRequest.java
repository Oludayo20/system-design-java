package com.systemdesign.blogstack.comments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "New comment payload.")
public record CreateCommentRequest(
        @Schema(example = "Great breakdown of the tradeoffs!") @NotBlank String body) {
}
