package com.systemdesign.orbit.core.testsupport;

import com.systemdesign.orbit.core.ports.out.PaymentGatewayPort;
import java.util.ArrayList;
import java.util.List;

/**
 * Trivial in-memory test double for the core's PaymentGatewayPort output port. Zero framework
 * imports, zero I/O — this is what makes it possible to unit test the core's business rules
 * without a database, without Spring, and without HTTP. See core/application/*Test.java.
 */
public class AlwaysSucceedsPaymentGateway implements PaymentGatewayPort {

    public record Charge(double amount, String customerId) {
    }

    public final List<Charge> charges = new ArrayList<>();

    @Override
    public ChargeResult charge(double amount, String customerId) {
        charges.add(new Charge(amount, customerId));
        return new ChargeResult(true, "fake_ref_" + charges.size());
    }
}
