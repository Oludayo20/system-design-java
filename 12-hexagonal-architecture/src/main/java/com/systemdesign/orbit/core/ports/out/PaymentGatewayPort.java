package com.systemdesign.orbit.core.ports.out;

/**
 * Output port: what the core needs from a payment provider. It knows nothing about Stripe or
 * Flutterwave — just this interface. Two adapters implement it (both mocked, no real network
 * calls): StripeMockAdapter and FlutterwaveMockAdapter.
 */
public interface PaymentGatewayPort {

    record ChargeResult(boolean success, String reference) {
    }

    ChargeResult charge(double amount, String customerId);
}
