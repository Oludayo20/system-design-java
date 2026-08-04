package com.systemdesign.asyncqueue.rides;

import java.util.UUID;

/** Direct port of the {@code CompleteRideResult} interface in rides.service.ts. */
public record CompleteRideResult(boolean success, UUID rideId) {
}
