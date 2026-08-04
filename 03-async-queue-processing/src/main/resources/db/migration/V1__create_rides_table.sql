-- Mirrors src/rides/entities/ride.entity.ts (TypeORM, synchronize: true in the original demo).
-- The Java port owns the schema explicitly via Flyway instead of Hibernate DDL generation.
CREATE TABLE rides (
    id                UUID PRIMARY KEY,
    rider_id          VARCHAR(255)   NOT NULL,
    driver_id         VARCHAR(255)   NOT NULL,
    fare              NUMERIC(10,2)  NOT NULL,
    pickup_location   VARCHAR(255)   NOT NULL,
    dropoff_location  VARCHAR(255)   NOT NULL,
    status            VARCHAR(50)    NOT NULL DEFAULT 'completed',
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);
