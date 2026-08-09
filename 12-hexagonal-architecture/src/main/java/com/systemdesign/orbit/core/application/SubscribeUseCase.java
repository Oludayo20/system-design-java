package com.systemdesign.orbit.core.application;

import com.systemdesign.orbit.core.domain.CustomerAlreadySubscribedError;
import com.systemdesign.orbit.core.domain.PaymentFailedError;
import com.systemdesign.orbit.core.domain.Plan;
import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.ports.in.SubscribePort;
import com.systemdesign.orbit.core.ports.out.NotifierPort;
import com.systemdesign.orbit.core.ports.out.PaymentGatewayPort;
import com.systemdesign.orbit.core.ports.out.SubscriptionRepositoryPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Implements the SubscribePort by orchestrating the output ports (repository, payment gateway,
 * notifier). This is the ONLY place those three interfaces meet — the use case doesn't know or
 * care whether the repository is Postgres or in-memory, or whether the gateway is Stripe or
 * Flutterwave. That wiring happens in config.CoreBeansConfig (HTTP) or the CLI's own bean lookup.
 */
public class SubscribeUseCase implements SubscribePort {

    private final SubscriptionRepositoryPort subscriptionRepository;
    private final PaymentGatewayPort paymentGateway;
    private final NotifierPort notifier;
    private final Supplier<String> idGenerator;
    private final Supplier<Instant> clock;

    public SubscribeUseCase(
            SubscriptionRepositoryPort subscriptionRepository,
            PaymentGatewayPort paymentGateway,
            NotifierPort notifier) {
        this(subscriptionRepository, paymentGateway, notifier, () -> UUID.randomUUID().toString(), Instant::now);
    }

    /** Test-friendly constructor: inject a deterministic id generator and/or clock. */
    public SubscribeUseCase(
            SubscriptionRepositoryPort subscriptionRepository,
            PaymentGatewayPort paymentGateway,
            NotifierPort notifier,
            Supplier<String> idGenerator,
            Supplier<Instant> clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.paymentGateway = paymentGateway;
        this.notifier = notifier;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Override
    public Subscription execute(SubscribeCommand command) {
        Instant now = clock.get();

        // Business rule 4: a customer cannot have two active subscriptions at once.
        Optional<Subscription> existing = subscriptionRepository.findByCustomerId(command.customerId());
        if (existing.isPresent() && existing.get().isActive(now)) {
            throw new CustomerAlreadySubscribedError(command.customerId());
        }

        Plan.PlanInfo plan = Plan.getPlan(command.planId());
        PaymentGatewayPort.ChargeResult charge = paymentGateway.charge(plan.price(), command.customerId());
        if (!charge.success()) {
            throw new PaymentFailedError("initial charge for customer " + command.customerId() + " was declined");
        }

        Subscription subscription = Subscription.create(idGenerator.get(), command.customerId(), command.planId(), now);

        subscriptionRepository.save(subscription);
        notifier.notify(
                command.customerId(),
                "Subscribed to " + plan.name() + " ($" + plan.price() + "/mo). Payment ref " + charge.reference() + ".");

        return subscription;
    }
}
