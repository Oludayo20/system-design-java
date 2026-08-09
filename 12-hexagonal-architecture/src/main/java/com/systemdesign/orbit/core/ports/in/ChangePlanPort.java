package com.systemdesign.orbit.core.ports.in;

import com.systemdesign.orbit.core.domain.Subscription;

/** Input port: upgrade or attempt-to-downgrade an existing subscription's plan. */
public interface ChangePlanPort {

    record ChangePlanCommand(String subscriptionId, String newPlanId) {
    }

    /** @param proratedAmount Amount charged immediately for the prorated upgrade (downgrades throw, never return 0). */
    record ChangePlanResult(Subscription subscription, double proratedAmount) {
    }

    ChangePlanResult execute(ChangePlanCommand command);
}
