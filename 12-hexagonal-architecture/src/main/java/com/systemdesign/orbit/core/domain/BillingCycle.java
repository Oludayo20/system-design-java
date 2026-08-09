package com.systemdesign.orbit.core.domain;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Pure billing-period math. No framework imports, no I/O — every method here is a plain function
 * of its inputs, which is what makes it trivial to unit test (see BillingCycleTest).
 */
public final class BillingCycle {

    public static final long MS_PER_DAY = 24L * 60 * 60 * 1000;

    /** Fixed-length billing periods keep proration math simple and deterministic. */
    public static final int BILLING_PERIOD_DAYS = 30;

    private BillingCycle() {
    }

    public static Instant addDays(Instant date, long days) {
        return date.plus(days, ChronoUnit.DAYS);
    }

    /** Whole days between two instants, rounded to the nearest day. */
    public static long daysBetween(Instant from, Instant to) {
        long millis = to.toEpochMilli() - from.toEpochMilli();
        return Math.round(millis / (double) MS_PER_DAY);
    }

    /** Days left in the billing period, clamped at zero once the period has ended. */
    public static long daysRemaining(Instant now, Instant currentPeriodEnd) {
        return Math.max(0, daysBetween(now, currentPeriodEnd));
    }

    public record BillingPeriod(Instant currentPeriodStart, Instant currentPeriodEnd) {
    }

    public static BillingPeriod startNewBillingPeriod(Instant now) {
        return new BillingPeriod(now, addDays(now, BILLING_PERIOD_DAYS));
    }

    public record ProrationInput(
            double oldPrice,
            double newPrice,
            Instant now,
            Instant currentPeriodStart,
            Instant currentPeriodEnd) {
    }

    /**
     * Upgrade proration: (newPrice - oldPrice) * daysRemaining / daysInPeriod, rounded to 2
     * decimals. Positive for upgrades (a bigger, more expensive plan) since newPrice > oldPrice.
     */
    public static double computeProration(ProrationInput input) {
        long daysInPeriod = Math.max(1, daysBetween(input.currentPeriodStart(), input.currentPeriodEnd()));
        long remaining = daysRemaining(input.now(), input.currentPeriodEnd());
        double rawAmount = ((input.newPrice() - input.oldPrice()) * remaining) / daysInPeriod;
        return Math.round(rawAmount * 100.0) / 100.0;
    }
}
