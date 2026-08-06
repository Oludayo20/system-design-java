package com.systemdesign.resilience.checkout;

import com.systemdesign.resilience.payment.PaymentResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "checkout", description = "Payment checkout with retries, circuit breaker, and fallback")
@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @PostMapping
    @Operation(
            summary = "Process a checkout payment",
            description = """
                    Flow: retry Paystack (MAX_RETRIES) → circuit breaker → Flutterwave fallback → queued/cached fallback.
                    Tune PAYMENT_FAILURE_RATE in application.properties to simulate flaky Paystack.""")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = PaymentResult.class)))
    public PaymentResult pay(@Valid @RequestBody CheckoutRequest body) {
        return checkout.checkout(body.amount());
    }

    @GetMapping("/circuit")
    @Operation(summary = "Inspect Paystack circuit breaker state", description = "Returns CLOSED, OPEN, or HALF_OPEN.")
    @ApiResponse(responseCode = "200", description = "Current circuit state.")
    public Map<String, String> circuitState() {
        return Map.of("paystackCircuit", checkout.getCircuitState());
    }
}
