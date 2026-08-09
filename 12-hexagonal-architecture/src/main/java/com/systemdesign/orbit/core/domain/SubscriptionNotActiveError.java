package com.systemdesign.orbit.core.domain;

public class SubscriptionNotActiveError extends DomainError {
    public SubscriptionNotActiveError(String subscriptionId) {
        super(
                "Subscription \"" + subscriptionId + "\" is not active (its billing period has ended).",
                "SUBSCRIPTION_NOT_ACTIVE");
    }
}
