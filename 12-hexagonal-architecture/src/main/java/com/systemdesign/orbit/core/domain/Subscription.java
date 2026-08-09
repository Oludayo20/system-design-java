package com.systemdesign.orbit.core.domain;

import java.time.Instant;

/**
 * The Orbit subscription aggregate. This is the entire business-rule surface of the domain — a
 * plain Java class with no annotations, no ORM, no HTTP. Everything it needs from the outside
 * world (persistence, payments, notifications) is a parameter, never an import.
 */
public final class Subscription {

    private final String id;
    private final String customerId;
    private String planId;
    private Instant currentPeriodStart;
    private Instant currentPeriodEnd;
    private boolean cancelAtPeriodEnd;
    private final Instant createdAt;
    private Instant updatedAt;

    private Subscription(
            String id,
            String customerId,
            String planId,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            boolean cancelAtPeriodEnd,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.planId = planId;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Starts a brand-new subscription, beginning a fresh billing period at {@code now}. */
    public static Subscription create(String id, String customerId, String planId, Instant now) {
        Plan.getPlan(planId); // throws UnknownPlanError for an invalid plan id
        BillingCycle.BillingPeriod period = BillingCycle.startNewBillingPeriod(now);
        return new Subscription(
                id, customerId, planId, period.currentPeriodStart(), period.currentPeriodEnd(), false, now, now);
    }

    /** Re-hydrates a subscription from persisted state. No business rules run here. */
    public static Subscription restore(
            String id,
            String customerId,
            String planId,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            boolean cancelAtPeriodEnd,
            Instant createdAt,
            Instant updatedAt) {
        return new Subscription(
                id, customerId, planId, currentPeriodStart, currentPeriodEnd, cancelAtPeriodEnd, createdAt,
                updatedAt);
    }

    /** True while the current billing period hasn't ended yet — cancellation-pending or not. */
    public boolean isActive(Instant now) {
        return now.isBefore(currentPeriodEnd);
    }

    public record PlanChangePreview(double proratedAmount) {
    }

    /**
     * Business rule 1: downgrading mid-cycle is rejected until {@code currentPeriodEnd}.
     * Business rule 2: upgrading mid-cycle is allowed and requires an immediate prorated charge.
     *
     * <p>This method only <em>previews</em> the change — it validates the rules and computes the
     * amount to charge, but does not mutate state. That lets a use case charge the payment
     * gateway first and only call {@link #applyPlanChange} once the charge succeeds, so a
     * declined payment never leaves the subscription half-upgraded.
     */
    public PlanChangePreview previewPlanChange(String newPlanId, Instant now) {
        if (!isActive(now)) {
            throw new SubscriptionNotActiveError(id);
        }

        int currentRank = Plan.planRank(planId);
        int newRank = Plan.planRank(newPlanId);

        if (newRank == currentRank) {
            throw new SamePlanError(id, newPlanId);
        }
        if (newRank < currentRank) {
            throw new DowngradeNotAllowedMidCycleError(id, currentPeriodEnd);
        }

        double proratedAmount = BillingCycle.computeProration(new BillingCycle.ProrationInput(
                Plan.getPlan(planId).price(),
                Plan.getPlan(newPlanId).price(),
                now,
                currentPeriodStart,
                currentPeriodEnd));

        return new PlanChangePreview(proratedAmount);
    }

    /** Applies an upgrade already validated (and paid for) via {@link #previewPlanChange}. */
    public void applyPlanChange(String newPlanId, Instant now) {
        this.planId = newPlanId;
        this.updatedAt = now;
    }

    /**
     * Business rule 3: cancelling schedules cancellation at period end rather than deleting or
     * deactivating anything immediately — the subscription stays active until currentPeriodEnd.
     */
    public void cancel(Instant now) {
        if (!isActive(now)) {
            throw new SubscriptionNotActiveError(id);
        }
        if (cancelAtPeriodEnd) {
            throw new AlreadyCanceledError(id);
        }
        this.cancelAtPeriodEnd = true;
        this.updatedAt = now;
    }

    public String getId() {
        return id;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getPlanId() {
        return planId;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
