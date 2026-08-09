package com.systemdesign.orbit.core.domain;

public class PaymentFailedError extends DomainError {
    public PaymentFailedError(String reason) {
        super("Payment failed: " + reason, "PAYMENT_FAILED");
    }
}
