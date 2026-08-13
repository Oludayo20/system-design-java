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
        // The id is an application-assigned UUID, never database-generated, so "does a row with
        // this id already exist" has to be answered explicitly — see the Persistable javadoc on
        // SubscriptionJpaEntity for why relying on JpaRepository's default isNew() heuristic
        // silently drops the insert.
        boolean isNew = !jpaRepository.existsById(subscription.getId());
        jpaRepository.saveAndFlush(toEntity(subscription, isNew));
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Subscription> findByCustomerId(String customerId) {
        return jpaRepository.findFirstByCustomerIdOrderByCreatedAtDesc(customerId).map(this::toDomain);
    }

    private SubscriptionJpaEntity toEntity(Subscription subscription, boolean isNew) {
        return new SubscriptionJpaEntity(
                isNew,
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
