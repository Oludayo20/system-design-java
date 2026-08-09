package com.systemdesign.orbit.core.domain;

public class SamePlanError extends DomainError {
    public SamePlanError(String subscriptionId, String planId) {
        super("Subscription \"" + subscriptionId + "\" is already on plan \"" + planId + "\".", "SAME_PLAN");
    }
}
