package com.systemdesign.captheorem.cap.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Partition toggle result.")
public record PartitionResponse(@Schema(example = "true") boolean partitioned) {}
