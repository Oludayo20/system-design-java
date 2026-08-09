package com.systemdesign.orbit.core.application;

import com.systemdesign.orbit.core.domain.PaymentFailedError;
import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.domain.SubscriptionNotFoundError;
import com.systemdesign.orbit.core.ports.in.ChangePlanPort;
import com.systemdesign.orbit.core.ports.out.NotifierPort;
import com.systemdesign.orbit.core.ports.out.PaymentGatewayPort;
import com.systemdesign.orbit.core.ports.out.SubscriptionRepositoryPort;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Implements ChangePlanPort. All the actual business rules (downgrade rejection, proration math)
 * live on the Subscription entity itself ({@code subscription.previewPlanChange}) — this use case
 * only orchestrates: load, preview, charge if needed, apply, persist, notify.
 */
public class ChangePlanUseCase implements ChangePlanPort {

    private final SubscriptionRepositoryPort subscriptionRepository;
    private final PaymentGatewayPort paymentGateway;
    private final NotifierPort notifier;
    private final Supplier<Instant> clock;

    public ChangePlanUseCase(
            SubscriptionRepositoryPort subscriptionRepository,
            PaymentGatewayPort paymentGateway,
            NotifierPort notifier) {
        this(subscriptionRepository, paymentGateway, notifier, Instant::now);
    }

    public ChangePlanUseCase(
            SubscriptionRepositoryPort subscriptionRepository,
            PaymentGatewayPort paymentGateway,
            NotifierPort notifier,
            Supplier<Instant> clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
        this.notifier = notifier;
        this.clock = clock;
    }

    @Override
    public ChangePlanResult execute(ChangePlanCommand command) {
        Instant now = clock.get();

        Subscription subscription = subscriptionRepository
                .findById(command.subscriptionId())
                .orElseThrow(() -> new SubscriptionNotFoundError(command.subscriptionId()));

        // Throws DowngradeNotAllowedMidCycleError / SamePlanError without touching state or money.
        Subscription.PlanChangePreview preview = subscription.previewPlanChange(command.newPlanId(), now);
        double proratedAmount = preview.proratedAmount();

        // Upgrades always have a positive proration (newPrice > oldPrice), so this always runs
        // for a successful upgrade — downgrades never reach here, previewPlanChange already threw.
        if (proratedAmount > 0) {
            PaymentGatewayPort.ChargeResult charge = paymentGateway.charge(proratedAmount, subscription.getCustomerId());
            if (!charge.success()) {
                throw new PaymentFailedError(
                        "prorated charge of $" + String.format("%.2f", proratedAmount)
                                + " for subscription " + subscription.getId() + " was declined");
            }
        }

        subscription.applyPlanChange(command.newPlanId(), now);
        subscriptionRepository.save(subscription);
        notifier.notify(
                subscription.getCustomerId(),
                "Plan changed to " + command.newPlanId() + ". Prorated charge: $"
                        + String.format("%.2f", proratedAmount) + ".");

        return new ChangePlanResult(subscription, proratedAmount);
    }
}
