package com.systemdesign.databasesharding.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Generates globally unique, roughly time-ordered IDs.
 *
 * Why not per-shard auto-increment? Because two shards would both hand out
 * id=1, id=2, ... and those IDs would collide the moment the system is
 * viewed as a whole (logs, analytics, the debug/distribution endpoint). The
 * fix used here - and in real sharded systems - is to mint the ID *before*
 * routing: generate a globally unique id first, hash that id to pick the
 * shard, then insert into that shard's table.
 *
 * This is a simplified Snowflake-style layout, identical in bit-width to
 * the TypeScript original (which deliberately stayed within a 53-bit
 * JS-safe integer even though Postgres BIGINT supports the full 64 bits):
 *   33 bits - seconds since a custom epoch (~272 years of headroom)
 *    8 bits - worker id (0-255), set via WORKER_ID env var
 *   11 bits - per-second sequence (0-2047), for bursts within one second
 *
 * Not a distributed-consensus ID service - for production scale you'd run a
 * real Snowflake/Sonyflake-style cluster or dedicated ID service. Out of
 * scope here, same as resharding (see README).
 *
 * Deviation from the original: {@link #nextId()} is {@code synchronized}.
 * The Node.js original relies on single-threaded event-loop execution for
 * {@code sequence}/{@code lastSecond} to be race-free; Spring MVC serves
 * requests from a thread pool, so the same mutable state needs an explicit
 * lock to stay correct under concurrent requests.
 */
@Service
public class IdGeneratorService {

    private static final long EPOCH_SECONDS = 1_700_000_000L; // fixed custom epoch
    private static final int WORKER_BITS = 8;
    private static final int SEQUENCE_BITS = 11;
    private static final int MAX_SEQUENCE = (1 << SEQUENCE_BITS) - 1;

    private final int workerId;
    private int sequence = 0;
    private long lastSecond = -1;

    public IdGeneratorService(@Value("${WORKER_ID:1}") int workerId) {
        this.workerId = workerId & 0xFF;
    }

    public synchronized long nextId() {
        long nowSeconds = Instant.now().getEpochSecond();

        if (nowSeconds == lastSecond) {
            sequence = (sequence + 1) & MAX_SEQUENCE;
            if (sequence == 0) {
                // Sequence exhausted for this second - spin to the next one.
                while (nowSeconds <= lastSecond) {
                    nowSeconds = Instant.now().getEpochSecond();
                }
            }
        } else {
            sequence = 0;
        }

        lastSecond = nowSeconds;

        long secondsPart = nowSeconds - EPOCH_SECONDS;
        long workerShifted = (long) workerId << SEQUENCE_BITS;
        long secondsShifted = secondsPart << (WORKER_BITS + SEQUENCE_BITS);

        return secondsShifted + workerShifted + sequence;
    }
}
