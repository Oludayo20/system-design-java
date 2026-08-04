package com.systemdesign.asyncqueue.rides;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/** Direct port of src/rides/entities/ride.entity.ts (TypeORM) onto Spring Data JPA. */
@Entity
@Table(name = "rides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ride {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "rider_id", nullable = false)
    private String riderId;

    @Column(name = "driver_id", nullable = false)
    private String driverId;

    // NOTE: TypeORM's `@Column('decimal')` maps to a JS `string` on the entity, to avoid float
    // rounding on read-back. Hibernate's idiomatic mapping for NUMERIC(10,2) is BigDecimal — used
    // here instead — and RidesService formats it to a fixed 2-decimal string only when building
    // the RideCompletedEvent payload, which is where the original's `string` typing actually
    // mattered (the wire format consumers see).
    @Column(name = "fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal fare;

    @Column(name = "pickup_location", nullable = false)
    private String pickupLocation;

    @Column(name = "dropoff_location", nullable = false)
    private String dropoffLocation;

    @Column(name = "status", nullable = false)
    @Builder.Default
    private String status = "completed";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
