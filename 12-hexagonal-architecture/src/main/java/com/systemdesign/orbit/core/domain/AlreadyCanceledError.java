package com.systemdesign.orbit.core.domain;

public class AlreadyCanceledError extends DomainError {
    public AlreadyCanceledError(String subscriptionId) {
        super(
                "Subscription \"" + subscriptionId + "\" is already scheduled to cancel at period end.",
                "ALREADY_CANCELED");
    }
}
