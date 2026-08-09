package com.systemdesign.orbit.adapters.in.http.dto;

import com.systemdesign.orbit.core.domain.Subscription;

public record ChangePlanResponse(
        String id,
        String customerId,
        String planId,
        String currentPeriodStart,
        String currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        String createdAt,
        String updatedAt,
        double proratedAmount) {

    public static ChangePlanResponse fromResult(Subscription subscription, double proratedAmount) {
        SubscriptionResponse base = SubscriptionResponse.fromDomain(subscription);
        return new ChangePlanResponse(
                base.id(),
                base.customerId(),
                base.planId(),
                base.currentPeriodStart(),
                base.currentPeriodEnd(),
                base.cancelAtPeriodEnd(),
                base.createdAt(),
                base.updatedAt(),
                proratedAmount);
    }
}
