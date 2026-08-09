package com.systemdesign.orbit.core.domain;

public class SubscriptionNotFoundError extends DomainError {
    public SubscriptionNotFoundError(String subscriptionId) {
        super("Subscription \"" + subscriptionId + "\" was not found.", "SUBSCRIPTION_NOT_FOUND");
    }
}
