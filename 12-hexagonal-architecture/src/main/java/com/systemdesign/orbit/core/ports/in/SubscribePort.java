package com.systemdesign.orbit.core.ports.in;

import com.systemdesign.orbit.core.domain.Subscription;

/**
 * Input port: what the core exposes to drive a new subscription. Inbound/driving adapters
 * (the REST controller, the CLI) call INTO the core through this interface.
 */
public interface SubscribePort {

    record SubscribeCommand(String customerId, String planId) {
    }

    Subscription execute(SubscribeCommand command);
}
