package com.systemdesign.orbit.core.ports.in;

import com.systemdesign.orbit.core.domain.Subscription;

/** Input port: schedule a subscription to cancel at the end of its current billing period. */
public interface CancelPort {

    record CancelCommand(String subscriptionId) {
    }

    Subscription execute(CancelCommand command);
}
