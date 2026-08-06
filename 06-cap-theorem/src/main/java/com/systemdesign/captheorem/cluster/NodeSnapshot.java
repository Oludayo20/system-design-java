package com.systemdesign.captheorem.cluster;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "In-memory database node snapshot.")
public record NodeSnapshot(
        @Schema(example = "A", allowableValues = {"A", "B"}) String name,
        @Schema(example = "1250") int productViews,
        @Schema(example = "5000") int walletBalance) {}
