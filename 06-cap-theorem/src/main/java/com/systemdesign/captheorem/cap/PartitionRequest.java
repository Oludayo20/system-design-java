package com.systemdesign.captheorem.cap;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Enable or disable simulated network partition.")
public record PartitionRequest(
        @Schema(example = "true", description = "When true, nodes A and B stop syncing.") @NotNull Boolean enabled) {}
