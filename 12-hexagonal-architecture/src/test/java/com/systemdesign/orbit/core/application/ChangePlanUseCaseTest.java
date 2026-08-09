package com.systemdesign.orbit.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.systemdesign.orbit.adapters.out.persistence.InMemorySubscriptionRepository;
import com.systemdesign.orbit.core.domain.DowngradeNotAllowedMidCycleError;
import com.systemdesign.orbit.core.domain.PaymentFailedError;
import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.ports.in.ChangePlanPort.ChangePlanCommand;
import com.systemdesign.orbit.core.ports.in.ChangePlanPort.ChangePlanResult;
import com.systemdesign.orbit.core.testsupport.AlwaysFailsPaymentGateway;
import com.systemdesign.orbit.core.testsupport.AlwaysSucceedsPaymentGateway;
import com.systemdesign.orbit.core.testsupport.RecordingNotifier;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Zero Postgres. Zero Spring context. Zero HTTP. See SubscribeUseCaseTest for why. */
class ChangePlanUseCaseTest {

    // Fixed, known instants make the proration math exactly checkable against the README's
    // formula: (newPrice - oldPrice) * daysRemaining / daysInPeriod.
    private static final Instant PERIOD_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-01-31T00:00:00Z"); // 30-day period
    private static final Instant NOW = Instant.parse("2026-01-16T00:00:00Z"); // exactly 15 days remaining

    private static Subscription seedSubscription(InMemorySubscriptionRepository repository, String planId) {
        Subscription subscription = Subscription.restore(
                "sub-1", "cust-1", planId, PERIOD_START, PERIOD_END, false, PERIOD_START, PERIOD_START);
        repository.save(subscription);
        return subscription;
    }

    @Test
    void upgradeChargesTheExactProratedAmountAndAppliesTheNewPlan_rule2() {
        InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
        seedSubscription(repository, "basic"); // $9/mo
        AlwaysSucceedsPaymentGateway gateway = new AlwaysSucceedsPaymentGateway();
        RecordingNotifier notifier = new RecordingNotifier();
        ChangePlanUseCase useCase = new ChangePlanUseCase(repository, gateway, notifier, () -> NOW);

        ChangePlanResult result = useCase.execute(new ChangePlanCommand("sub-1", "pro")); // $29/mo

        // (29 - 9) * 15 / 30 = 10.00 — matches the formula in the README exactly.
        assertEquals(10.0, result.proratedAmount());
        assertEquals("pro", result.subscription().getPlanId());
        assertEquals(1, gateway.charges.size());
        assertEquals(10.0, gateway.charges.get(0).amount());
        assertEquals("cust-1", gateway.charges.get(0).customerId());

        Subscription persisted = repository.findById("sub-1").orElseThrow();
        assertEquals("pro", persisted.getPlanId());
    }

    @Test
    void downgradeRejectedMidCycleWithNoChargeAndNoStateChange_rule1() {
        InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
        seedSubscription(repository, "enterprise");
        AlwaysSucceedsPaymentGateway gateway = new AlwaysSucceedsPaymentGateway();
        RecordingNotifier notifier = new RecordingNotifier();
        ChangePlanUseCase useCase = new ChangePlanUseCase(repository, gateway, notifier, () -> NOW);

        assertThrows(
                DowngradeNotAllowedMidCycleError.class,
                () -> useCase.execute(new ChangePlanCommand("sub-1", "pro")));

        assertEquals(0, gateway.charges.size());
        assertEquals(0, notifier.messages.size());
        Subscription persisted = repository.findById("sub-1").orElseThrow();
        assertEquals("enterprise", persisted.getPlanId()); // unchanged
    }

    @Test
    void upgradeLeavesThePlanUnchangedWhenTheProrationChargeIsDeclined() {
        InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
        seedSubscription(repository, "basic");
        AlwaysFailsPaymentGateway gateway = new AlwaysFailsPaymentGateway();
        RecordingNotifier notifier = new RecordingNotifier();
        ChangePlanUseCase useCase = new ChangePlanUseCase(repository, gateway, notifier, () -> NOW);

        assertThrows(PaymentFailedError.class, () -> useCase.execute(new ChangePlanCommand("sub-1", "pro")));

        Subscription persisted = repository.findById("sub-1").orElseThrow();
        assertEquals("basic", persisted.getPlanId()); // charge declined -> plan never changed
    }
}
