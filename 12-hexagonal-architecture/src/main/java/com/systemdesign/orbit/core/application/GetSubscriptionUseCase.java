package com.systemdesign.orbit.core.application;

import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.domain.SubscriptionNotFoundError;
import com.systemdesign.orbit.core.ports.in.GetSubscriptionPort;
import com.systemdesign.orbit.core.ports.out.SubscriptionRepositoryPort;

/** Implements GetSubscriptionPort. A read-only use case backing {@code GET /subscriptions/{id}}. */
public class GetSubscriptionUseCase implements GetSubscriptionPort {

    private final SubscriptionRepositoryPort subscriptionRepository;

    public GetSubscriptionUseCase(SubscriptionRepositoryPort subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @Override
    public Subscription execute(GetSubscriptionQuery query) {
        return subscriptionRepository
                .findById(query.subscriptionId())
                .orElseThrow(() -> new SubscriptionNotFoundError(query.subscriptionId()));
    }
}
