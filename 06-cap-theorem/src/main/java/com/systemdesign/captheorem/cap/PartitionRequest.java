package com.systemdesign.captheorem.cap;

import jakarta.validation.constraints.NotNull;

public record PartitionRequest(@NotNull Boolean enabled) {}
