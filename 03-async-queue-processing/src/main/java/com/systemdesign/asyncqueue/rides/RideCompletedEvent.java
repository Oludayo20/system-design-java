package com.systemdesign.asyncqueue.rides;

/**
 * Payload published to the ride_events exchange with routing key "ride.completed". Direct port
 * of src/rides/ride-completed.event.ts — {@code fare} stays a String on the wire, matching the
 * original TypeORM decimal-as-string convention, even though the JPA entity itself uses
 * BigDecimal (see the note on {@link Ride#getFare()}).
 */
public record RideCompletedEvent(
        String rideId,
        String riderId,
        String driverId,
        String fare,
        String pickupLocation,
        String dropoffLocation,
        String completedAt
) {
}
