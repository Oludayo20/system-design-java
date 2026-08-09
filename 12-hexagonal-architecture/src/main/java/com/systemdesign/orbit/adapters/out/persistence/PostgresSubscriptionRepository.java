package com.systemdesign.orbit.adapters.out.persistence;

import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.ports.out.SubscriptionRepositoryPort;
import java.util.Optional;

/**
 * Outbound/driven adapter #2 for SubscriptionRepositoryPort — real Postgres via Spring Data JPA.
 * Implements the exact same port as InMemorySubscriptionRepository. Selected via
 * {@code app.repository=postgres}. The core never sees {@code SubscriptionJpaEntity} or a
 * Spring Data {@code JpaRepository}.
 */
public class PostgresSubscriptionRepository implements SubscriptionRepositoryPort {

    private final SubscriptionJpaRepository jpaRepository;

    public PostgresSubscriptionRepository(SubscriptionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(Subscription subscription) {
        jpaRepository.save(toEntity(subscription));
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Subscription> findByCustomerId(String customerId) {
        return jpaRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId).map(this::toDomain);
    }

    private SubscriptionJpaEntity toEntity(Subscription subscription) {
        return new SubscriptionJpaEntity(
                subscription.getId(),
                subscription.getCustomerId(),
                subscription.getPlanId(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCreatedAt(),
                subscription.getUpdatedAt());
    }

    private Subscription toDomain(SubscriptionJpaEntity entity) {
        return Subscription.restore(
                entity.getId(),
                entity.getCustomerId(),
                entity.getPlanId(),
                entity.getCurrentPeriodStart(),
                entity.getCurrentPeriodEnd(),
                entity.isCancelAtPeriodEnd(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
