package com.systemdesign.orbit.core.domain;

import java.time.Instant;

/** Business rule 1: downgrading mid-cycle is not allowed until the current period ends. */
public class DowngradeNotAllowedMidCycleError extends DomainError {
    public DowngradeNotAllowedMidCycleError(String subscriptionId, Instant currentPeriodEnd) {
        super(
                "Subscription \"" + subscriptionId + "\" cannot downgrade mid-cycle. "
                        + "Downgrades take effect at the end of the current billing period ("
                        + currentPeriodEnd + ").",
                "DOWNGRADE_NOT_ALLOWED_MID_CYCLE");
    }
}
