package com.systemdesign.resilience.checkout;

import com.systemdesign.resilience.payment.PaymentResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "checkout")
@RestController
@RequestMapping("/checkout")
public class CheckoutController {

    private final CheckoutService checkout;

    public CheckoutController(CheckoutService checkout) {
        this.checkout = checkout;
    }

    @PostMapping
    public PaymentResult pay(@Valid @RequestBody CheckoutRequest body) {
        return checkout.checkout(body.amount());
    }

    @GetMapping("/circuit")
    public Map<String, String> circuitState() {
        return Map.of("paystackCircuit", checkout.getCircuitState());
    }
}
