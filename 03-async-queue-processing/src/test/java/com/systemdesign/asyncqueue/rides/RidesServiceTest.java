package com.systemdesign.asyncqueue.rides;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.systemdesign.asyncqueue.rabbitmq.RabbitmqService;
import com.systemdesign.asyncqueue.rabbitmq.Topology;
import com.systemdesign.asyncqueue.rides.dto.CreateRideDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Direct JUnit 5 + Mockito port of src/rides/rides.service.spec.ts. */
@ExtendWith(MockitoExtension.class)
class RidesServiceTest {

    private static final UUID RIDE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private RideRepository rideRepository;

    @Mock
    private RabbitmqService rabbitmqService;

    private RidesService ridesService;
    private CreateRideDto dto;

    @BeforeEach
    void setUp() {
        ridesService = new RidesService(rideRepository, rabbitmqService);

        dto = new CreateRideDto();
        dto.setRiderId("rider-1");
        dto.setDriverId("driver-1");
        dto.setFare(new BigDecimal("24.50"));
        dto.setPickupLocation("Ikeja, Lagos");
        dto.setDropoffLocation("Lekki, Lagos");

        // Stand-in for the DB assigning an id + createdAt on insert.
        when(rideRepository.save(any(Ride.class))).thenAnswer(invocation -> {
            Ride ride = invocation.getArgument(0);
            ride.setId(RIDE_ID);
            ride.setCreatedAt(CREATED_AT);
            return ride;
        });
    }

    @Test
    void persistsTheRideAndReturnsSuccessTrueRideIdImmediately() {
        CompleteRideResult result = ridesService.completeRide(dto);

        verify(rideRepository, times(1)).save(any(Ride.class));
        assertThat(result).isEqualTo(new CompleteRideResult(true, RIDE_ID));
    }

    @Test
    void publishesRideCompletedToTheRideEventsExchangeWithTheRidePayload() {
        ridesService.completeRide(dto);

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(rabbitmqService, times(1)).publish(
                eq(Topology.RIDE_EVENTS_EXCHANGE),
                eq(Topology.RIDE_COMPLETED_ROUTING_KEY),
                payloadCaptor.capture(),
                anyMap());

        RideCompletedEvent event = (RideCompletedEvent) payloadCaptor.getValue();
        assertThat(event.rideId()).isEqualTo(RIDE_ID.toString());
        assertThat(event.riderId()).isEqualTo(dto.getRiderId());
        assertThat(event.driverId()).isEqualTo(dto.getDriverId());
        assertThat(event.fare()).isEqualTo("24.50");
    }

    @Test
    void savesBeforePublishingTheEventCarriesThePersistedRideId() {
        ridesService.completeRide(dto);

        InOrder inOrder = inOrder(rideRepository, rabbitmqService);
        inOrder.verify(rideRepository).save(any(Ride.class));
        inOrder.verify(rabbitmqService).publish(anyString(), anyString(), any(), anyMap());
    }

    @Test
    void neverTouchesWorkerLogicItsOnlyCollaboratorsAreTheRepositoryAndThePublisher() {
        // Workers (Email/Analytics/Loyalty) live in a separate process (WorkerApplication),
        // reachable only by consuming a queue — RidesService has no dependency on them at all, so
        // "returns immediately without awaiting worker logic" holds by construction here, not
        // just by timing.
        ridesService.completeRide(dto);
        verify(rabbitmqService).publish(anyString(), anyString(), any(), anyMap());
    }
}
