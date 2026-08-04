package com.systemdesign.asyncqueue.rides;

import java.math.RoundingMode;
import java.util.Map;

import com.systemdesign.asyncqueue.rabbitmq.RabbitmqService;
import com.systemdesign.asyncqueue.rabbitmq.Topology;
import com.systemdesign.asyncqueue.rides.dto.CreateRideDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Direct port of src/rides/rides.service.ts.
 *
 * <p>Mirrors the doc's Uber example: persist the trip (the one thing the rider needs to be
 * durable before we respond), publish ride.completed, and return. We deliberately do not call —
 * let alone await — sendEmail/generateReceipt/updateAnalytics/awardLoyaltyPoints here; those only
 * exist inside the worker process, reached solely by consuming the queues that ride_events fans
 * out to. That is what keeps this handler in the ~200ms range instead of the 6-7s a fully-serial
 * implementation would take.
 */
@Service
public class RidesService {

    private static final Logger log = LoggerFactory.getLogger(RidesService.class);

    private final RideRepository rides;
    private final RabbitmqService rabbitmq;

    public RidesService(RideRepository rides, RabbitmqService rabbitmq) {
        this.rides = rides;
        this.rabbitmq = rabbitmq;
    }

    public CompleteRideResult completeRide(CreateRideDto dto) {
        Ride ride = rides.save(
                Ride.builder()
                        .riderId(dto.getRiderId())
                        .driverId(dto.getDriverId())
                        .fare(dto.getFare().setScale(2, RoundingMode.HALF_UP))
                        .pickupLocation(dto.getPickupLocation())
                        .dropoffLocation(dto.getDropoffLocation())
                        .status("completed")
                        .build());

        RideCompletedEvent event = new RideCompletedEvent(
                ride.getId().toString(),
                ride.getRiderId(),
                ride.getDriverId(),
                ride.getFare().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                ride.getPickupLocation(),
                ride.getDropoffLocation(),
                // Instant#toString() renders ISO-8601 with a trailing "Z", matching TypeORM's
                // Date#toISOString() closely enough for this demo payload (both are UTC instants).
                ride.getCreatedAt().toString());

        rabbitmq.publish(Topology.RIDE_EVENTS_EXCHANGE, Topology.RIDE_COMPLETED_ROUTING_KEY, event, Map.of());
        log.info("Ride {} completed and ride.completed published", ride.getId());

        return new CompleteRideResult(true, ride.getId());
    }
}
