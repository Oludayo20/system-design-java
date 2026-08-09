package com.systemdesign.blogstack.posts.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "New post payload.")
public record CreatePostRequest(
        @Schema(example = "Why plain monoliths still ship products") @NotBlank @Size(max = 200) String title,
        @Schema(example = "A monolith is just one app, one repo, one deploy...") @NotBlank String body) {
}
