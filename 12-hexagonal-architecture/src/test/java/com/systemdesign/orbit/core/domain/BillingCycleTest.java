package com.systemdesign.orbit.core.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Pure math, zero I/O — see billing-cycle.spec.ts in the TypeScript sibling for the same cases. */
class BillingCycleTest {

    @Test
    void computesNewMinusOldTimesDaysRemainingOverDaysInPeriodRoundedToTwoDecimals() {
        Instant currentPeriodStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant currentPeriodEnd = Instant.parse("2026-01-31T00:00:00Z"); // 30-day period
        Instant now = Instant.parse("2026-01-16T00:00:00Z"); // 15 days remaining

        double amount = BillingCycle.computeProration(
                new BillingCycle.ProrationInput(9, 29, now, currentPeriodStart, currentPeriodEnd));

        // (29 - 9) * 15 / 30 = 10.00
        assertEquals(10.0, amount);
    }

    @Test
    void roundsToTwoDecimalPlacesForUnevenSplits() {
        Instant currentPeriodStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant currentPeriodEnd = Instant.parse("2026-01-31T00:00:00Z"); // 30-day period
        Instant now = Instant.parse("2026-01-21T00:00:00Z"); // 10 days remaining

        double amount = BillingCycle.computeProration(
                new BillingCycle.ProrationInput(29, 99, now, currentPeriodStart, currentPeriodEnd));

        // (99 - 29) * 10 / 30 = 23.333... -> 23.33
        assertEquals(23.33, amount);
    }

    @Test
    void isFullPriceDiffAtTheVeryStartOfThePeriod() {
        Instant currentPeriodStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant currentPeriodEnd = Instant.parse("2026-01-31T00:00:00Z");

        double amount = BillingCycle.computeProration(
                new BillingCycle.ProrationInput(9, 29, currentPeriodStart, currentPeriodStart, currentPeriodEnd));

        // (29 - 9) * 30 / 30 = 20.00
        assertEquals(20.0, amount);
    }

    @Test
    void clampsToZeroOncePeriodHasEnded() {
        Instant currentPeriodStart = Instant.parse("2026-01-01T00:00:00Z");
        Instant currentPeriodEnd = Instant.parse("2026-01-31T00:00:00Z");
        Instant now = Instant.parse("2026-02-15T00:00:00Z"); // well past period end

        double amount = BillingCycle.computeProration(
                new BillingCycle.ProrationInput(9, 29, now, currentPeriodStart, currentPeriodEnd));

        assertEquals(0.0, amount);
    }

    @Test
    void countsWholeDaysBetweenTwoInstants() {
        assertEquals(30, BillingCycle.daysBetween(Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-31T00:00:00Z")));
    }

    @Test
    void neverReturnsNegativeDaysRemaining() {
        Instant past = Instant.parse("2026-01-01T00:00:00Z");
        Instant future = Instant.parse("2025-01-01T00:00:00Z");
        assertEquals(0, BillingCycle.daysRemaining(past, future));
    }
}
