package com.systemdesign.orbit.core.testsupport;

import com.systemdesign.orbit.core.ports.out.PaymentGatewayPort;
import java.util.ArrayList;
import java.util.List;

public class AlwaysFailsPaymentGateway implements PaymentGatewayPort {

    public record Charge(double amount, String customerId) {
    }

    public final List<Charge> charges = new ArrayList<>();

    @Override
    public ChargeResult charge(double amount, String customerId) {
        charges.add(new Charge(amount, customerId));
        return new ChargeResult(false, "fake_declined");
    }
}
