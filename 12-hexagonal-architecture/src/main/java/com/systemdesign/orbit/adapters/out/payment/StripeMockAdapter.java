package com.systemdesign.orbit.adapters.out.payment;

import com.systemdesign.orbit.core.ports.out.PaymentGatewayPort;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Outbound/driven adapter #1 for PaymentGatewayPort — a simulated Stripe. No real network call:
 * a small random latency stands in for the round trip, and a small DETERMINISTIC (not random)
 * failure rate makes the failure mode reproducible in demos and tests instead of flaky.
 * Selected via {@code app.payment-provider=stripe} (the default).
 */
public class StripeMockAdapter implements PaymentGatewayPort {

    private final AtomicInteger callCount = new AtomicInteger();

    @Override
    public ChargeResult charge(double amount, String customerId) {
        int count = callCount.incrementAndGet();

        sleep(20 + Math.round(Math.random() * 60));

        // Every 11th charge on this adapter instance is declined — deterministic, not a dice roll.
        boolean declined = count % 11 == 0;
        if (declined) {
            return new ChargeResult(false, "stripe_declined_" + count);
        }

        return new ChargeResult(
                true,
                "pi_stripe_" + UUID.randomUUID() + "_" + customerId + "_" + String.format("%.2f", amount));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
