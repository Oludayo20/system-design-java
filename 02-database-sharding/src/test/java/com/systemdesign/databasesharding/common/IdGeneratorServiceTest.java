package com.systemdesign.databasesharding.common;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ported from id-generator.service.spec.ts. */
class IdGeneratorServiceTest {

    @Test
    void generatesUniqueIdsUnderATightLoop() {
        IdGeneratorService generator = new IdGeneratorService(1);
        Set<Long> ids = new HashSet<>();

        for (int i = 0; i < 5000; i++) {
            long id = generator.nextId();
            assertTrue(id >= 0);
            ids.add(id);
        }

        assertEquals(5000, ids.size());
    }

    @Test
    void producesIncreasingIdsOverTime() {
        IdGeneratorService generator = new IdGeneratorService(1);
        long first = generator.nextId();
        long second = generator.nextId();
        assertTrue(second > first);
    }
}
