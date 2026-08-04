package com.systemdesign.resilience.checkout;

import com.systemdesign.resilience.payment.FlakyPaymentGateway;
import com.systemdesign.resilience.payment.PaymentResult;
import com.systemdesign.resilience.resilience.CircuitBreaker;
import com.systemdesign.resilience.resilience.CircuitBreakerOptions;
import com.systemdesign.resilience.resilience.RetryUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CheckoutService {

    private final FlakyPaymentGateway payments;
    private final CircuitBreaker paystackBreaker;
    private final int maxRetries;
    private final long retryDelayMs;

    public CheckoutService(
            FlakyPaymentGateway payments,
            @Value("${circuit.failure-threshold}") int circuitFailureThreshold,
            @Value("${circuit.reset-ms}") long circuitResetMs,
            @Value("${retry.max-attempts}") int maxRetries,
            @Value("${retry.delay-ms}") long retryDelayMs) {
        this.payments = payments;
        this.paystackBreaker = new CircuitBreaker(
                new CircuitBreakerOptions(circuitFailureThreshold, circuitResetMs));
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    public PaymentResult checkout(double amount) {
        try {
            return paystackBreaker.execute(() -> RetryUtil.withRetries(
                    () -> payments.chargePaystack(amount),
                    new RetryUtil.RetryOptions(maxRetries, retryDelayMs)));
        } catch (RuntimeException ignored) {
            try {
                return payments.chargeFlutterwave(amount);
            } catch (RuntimeException fallbackIgnored) {
                return payments.cachedFallback(amount);
            }
        }
    }

    public String getCircuitState() {
        return paystackBreaker.getState().name();
    }
}
