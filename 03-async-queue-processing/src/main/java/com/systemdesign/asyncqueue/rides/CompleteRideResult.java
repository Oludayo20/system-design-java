package com.systemdesign.asyncqueue.rides;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Ride saved and ride.completed published. Workers process asynchronously.")
public record CompleteRideResult(
        @Schema(example = "true") boolean success,
        @Schema(example = "5f1b6b2e-8c4d-4a1e-9f3b-2d7e6a5b4c3d") UUID rideId) {
}
