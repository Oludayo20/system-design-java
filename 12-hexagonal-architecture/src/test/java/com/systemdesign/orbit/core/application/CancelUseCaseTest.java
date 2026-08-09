package com.systemdesign.orbit.core.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.systemdesign.orbit.adapters.out.persistence.InMemorySubscriptionRepository;
import com.systemdesign.orbit.core.domain.AlreadyCanceledError;
import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.ports.in.CancelPort.CancelCommand;
import com.systemdesign.orbit.core.testsupport.RecordingNotifier;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Zero Postgres. Zero Spring context. Zero HTTP. See SubscribeUseCaseTest for why. */
class CancelUseCaseTest {

    private static final Instant PERIOD_START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant PERIOD_END = Instant.parse("2026-01-31T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-01-16T00:00:00Z"); // mid-cycle

    private static void seedSubscription(InMemorySubscriptionRepository repository) {
        repository.save(Subscription.restore(
                "sub-1", "cust-1", "pro", PERIOD_START, PERIOD_END, false, PERIOD_START, PERIOD_START));
    }

    @Test
    void setsCancelAtPeriodEndWithoutDeactivatingTheSubscriptionImmediately_rule3() {
        InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
        seedSubscription(repository);
        RecordingNotifier notifier = new RecordingNotifier();
        CancelUseCase useCase = new CancelUseCase(repository, notifier, () -> NOW);

        Subscription result = useCase.execute(new CancelCommand("sub-1"));

        assertTrue(result.isCancelAtPeriodEnd());
        assertEquals(PERIOD_END, result.getCurrentPeriodEnd()); // untouched — nothing deleted
        assertTrue(result.isActive(NOW)); // still active until period end
        assertEquals(1, notifier.messages.size());

        Subscription persisted = repository.findById("sub-1").orElseThrow();
        assertTrue(persisted.isCancelAtPeriodEnd());
    }

    @Test
    void rejectsCancellingASubscriptionThatIsAlreadyScheduledToCancel() {
        InMemorySubscriptionRepository repository = new InMemorySubscriptionRepository();
        seedSubscription(repository);
        RecordingNotifier notifier = new RecordingNotifier();
        CancelUseCase useCase = new CancelUseCase(repository, notifier, () -> NOW);

        useCase.execute(new CancelCommand("sub-1"));

        assertThrows(AlreadyCanceledError.class, () -> useCase.execute(new CancelCommand("sub-1")));
    }
}
