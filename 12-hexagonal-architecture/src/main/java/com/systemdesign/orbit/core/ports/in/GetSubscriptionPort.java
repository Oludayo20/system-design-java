package com.systemdesign.orbit.core.ports.in;

import com.systemdesign.orbit.core.domain.Subscription;

/** Input port: read a single subscription by id. Backs {@code GET /subscriptions/{id}}. */
public interface GetSubscriptionPort {

    record GetSubscriptionQuery(String subscriptionId) {
    }

    Subscription execute(GetSubscriptionQuery query);
}
