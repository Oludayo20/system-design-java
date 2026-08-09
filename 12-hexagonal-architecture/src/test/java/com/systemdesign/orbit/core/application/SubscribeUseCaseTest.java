package com.systemdesign.orbit.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.systemdesign.orbit.adapters.out.persistence.InMemorySubscriptionRepository;
import com.systemdesign.orbit.core.domain.CustomerAlreadySubscribedError;
import com.systemdesign.orbit.core.domain.PaymentFailedError;
import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.ports.in.SubscribePort.SubscribeCommand;
import com.systemdesign.orbit.core.testsupport.AlwaysFailsPaymentGateway;
import com.systemdesign.orbit.core.testsupport.AlwaysSucceedsPaymentGateway;
import com.systemdesign.orbit.core.testsupport.RecordingNotifier;
import org.junit.jupiter.api.Test;

/**
 * Zero Postgres. Zero Spring context. Zero HTTP. Just the core use case, the real
 * InMemorySubscriptionRepository adapter, and trivial fake payment/notifier doubles — all wired
 * with plain {@code new}. This is the payoff of hexagonal architecture: the core's business rules
 * are testable in milliseconds, with no infrastructure standing between the test and the rule.
 */
class SubscribeUseCaseTest {

    @Test
    void createsANewActiveSubscriptionAndChargesThePlanPrice() {
        InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
        AlwaysSucceedsPaymentGateway gateway = new AlwaysSucceedsPaymentGateway();
        RecordingNotifier notifier = new RecordingNotifier();
        SubscribeUseCase useCase = new SubscribeUseCase(repository, gateway, notifier);

        Subscription subscription = useCase.execute(new SubscribeCommand("cust-1", "pro"));

        assertEquals("cust-1", subscription.getCustomerId());
        assertEquals("pro", subscription.getPlanId());
        assertFalse(subscription.isCancelAtPeriodEnd());
        assertEquals(1, gateway.charges.size());
        assertEquals(29.0, gateway.charges.get(0).amount());
        assertEquals("cust-1", gateway.charges.get(0).customerId());
        assertEquals(1, notifier.messages.size());
        assertTrue(repository.findById(subscription.getId()).isPresent());
    }

    @Test
    void rejectsSubscribingACustomerWhoAlreadyHasAnActiveSubscription_rule4() {
        InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
        AlwaysSucceedsPaymentGateway gateway = new AlwaysSucceedsPaymentGateway();
        RecordingNotifier notifier = new RecordingNotifier();
        SubscribeUseCase useCase = new SubscribeUseCase(repository, gateway, notifier);

        useCase.execute(new SubscribeCommand("cust-1", "basic"));

        assertThrows(
                CustomerAlreadySubscribedError.class,
                () -> useCase.execute(new SubscribeCommand("cust-1", "pro")));

        // Still only the first subscription — the rejected attempt never touched the repository.
        Subscription stored = repository.findByCustomerId("cust-1").orElseThrow();
        assertEquals("basic", stored.getPlanId());
    }

    @Test
    void doesNotCreateASubscriptionWhenThePaymentGatewayDeclinesTheCharge() {
        InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
        AlwaysFailsPaymentGateway gateway = new AlwaysFailsPaymentGateway();
        RecordingNotifier notifier = new RecordingNotifier();
        SubscribeUseCase useCase = new SubscribeUseCase(repository, gateway, notifier);

        assertThrows(PaymentFailedError.class, () -> useCase.execute(new SubscribeCommand("cust-2", "basic")));

        assertTrue(repository.findByCustomerId("cust-2").isEmpty());
        assertEquals(0, notifier.messages.size());
    }
}
