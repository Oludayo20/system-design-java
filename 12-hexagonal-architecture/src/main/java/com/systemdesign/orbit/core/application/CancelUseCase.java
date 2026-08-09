package com.systemdesign.orbit.core.application;

import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.domain.SubscriptionNotFoundError;
import com.systemdesign.orbit.core.ports.in.CancelPort;
import com.systemdesign.orbit.core.ports.out.NotifierPort;
import com.systemdesign.orbit.core.ports.out.SubscriptionRepositoryPort;
import java.time.Instant;
import java.util.function.Supplier;

/** Implements CancelPort. The "stays active until period end" rule lives on Subscription.cancel(). */
public class CancelUseCase implements CancelPort {

    private final SubscriptionRepositoryPort subscriptionRepository;
    private final NotifierPort notifier;
    private final Supplier<Instant> clock;

    public CancelUseCase(SubscriptionRepositoryPort subscriptionRepository, NotifierPort notifier) {
        this(subscriptionRepository, notifier, Instant::now);
    }

    public CancelUseCase(
            SubscriptionRepositoryPort subscriptionRepository, NotifierPort notifier, Supplier<Instant> clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Override
    public Subscription execute(CancelCommand command) {
        Instant now = clock.get();

        Subscription subscription = subscriptionRepository
                .findById(command.subscriptionId())
                .orElseThrow(() -> new SubscriptionNotFoundError(command.subscriptionId()));

        subscription.cancel(now);
        subscriptionRepository.save(subscription);
        notifier.notify(
                subscription.getCustomerId(),
                "Subscription will cancel at period end (" + subscription.getCurrentPeriodEnd()
                        + "). Access continues until then.");

        return subscription;
    }
}
