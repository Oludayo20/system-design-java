package com.systemdesign.resilience.payment;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FlakyPaymentGateway {

    private int attemptCounter = 0;

    private final double failureRate;

    public FlakyPaymentGateway(@Value("${payment.failure-rate}") double failureRate) {
        this.failureRate = failureRate;
    }

    public PaymentResult chargePaystack(double amount) {
        attemptCounter += 1;

        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new IllegalStateException("Paystack timeout");
        }

        return new PaymentResult(
                "paystack",
                "paystack-" + attemptCounter + "-" + amount,
                "success");
    }

    public PaymentResult chargeFlutterwave(double amount) {
        return new PaymentResult("flutterwave", "flutterwave-" + amount, "success");
    }

    public PaymentResult cachedFallback(double amount) {
        return new PaymentResult("cached-fallback", "queued-" + amount, "queued");
    }
}
