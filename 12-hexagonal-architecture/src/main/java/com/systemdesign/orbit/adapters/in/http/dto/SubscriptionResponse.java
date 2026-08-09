package com.systemdesign.orbit.adapters.in.http.dto;

import com.systemdesign.orbit.core.domain.Subscription;

public record SubscriptionResponse(
        String id,
        String customerId,
        String planId,
        String currentPeriodStart,
        String currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        String createdAt,
        String updatedAt) {

    public static SubscriptionResponse fromDomain(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCustomerId(),
                subscription.getPlanId(),
                subscription.getCurrentPeriodStart().toString(),
                subscription.getCurrentPeriodEnd().toString(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCreatedAt().toString(),
                subscription.getUpdatedAt().toString());
    }
}
