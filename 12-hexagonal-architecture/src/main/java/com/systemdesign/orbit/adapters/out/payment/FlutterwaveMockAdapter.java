package com.systemdesign.orbit.adapters.out.payment;

import com.systemdesign.orbit.core.ports.out.PaymentGatewayPort;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Outbound/driven adapter #2 for PaymentGatewayPort — a simulated Flutterwave. Implements the
 * exact same port as StripeMockAdapter with different latency/failure characteristics, proving
 * providers are swappable without the core (or the use cases) knowing or caring which one runs.
 * Selected via {@code app.payment-provider=flutterwave}.
 */
public class FlutterwaveMockAdapter implements PaymentGatewayPort {

    private final AtomicInteger callCount = new AtomicInteger();

    @Override
    public ChargeResult charge(double amount, String customerId) {
        int count = callCount.incrementAndGet();

        sleep(15 + Math.round(Math.random() * 50));

        // Every 13th charge on this adapter instance is declined — a different deterministic
        // cadence than Stripe's, just to make the two adapters observably distinct in a demo.
        boolean declined = count % 13 == 0;
        if (declined) {
            return new ChargeResult(false, "flw_declined_" + count);
        }

        return new ChargeResult(
                true, "flw_" + UUID.randomUUID() + "_" + customerId + "_" + String.format("%.2f", amount));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
