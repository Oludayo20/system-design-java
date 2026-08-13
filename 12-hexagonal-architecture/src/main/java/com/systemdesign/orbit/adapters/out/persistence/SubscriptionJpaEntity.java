package com.systemdesign.orbit.adapters.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import org.springframework.data.domain.Persistable;

/**
 * JPA entity. This is the ONLY place {@code jakarta.persistence} annotations appear for
 * subscriptions — the domain's {@code Subscription} class (core/domain/Subscription.java) has
 * never heard of JPA. PostgresSubscriptionRepository maps between the two.
 *
 * <p>Implements {@link Persistable} because {@code id} is an application-assigned (not
 * database-generated) key: Spring Data JPA's default new-vs-existing heuristic is "id == null
 * means new," which is never true here (Subscription.create() always assigns a UUID before the
 * entity exists). Without this, {@code JpaRepository.save()} always calls {@code merge()}, and
 * merging a row that doesn't exist yet is a silent no-op instead of an insert — see
 * PostgresSubscriptionRepository#save, which sets {@code isNew} from an explicit
 * {@code existsById} check.
 */
@Entity
@Table(name = "subscriptions")
public class SubscriptionJpaEntity implements Persistable<String> {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "customer_id", length = 100, nullable = false)
    private String customerId;

    @Column(name = "plan_id", length = 20, nullable = false)
    private String planId;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Transient
    private boolean isNew;

    /** No-arg constructor required by JPA. */
    protected SubscriptionJpaEntity() {
    }

    public SubscriptionJpaEntity(
            boolean isNew,
            String id,
            String customerId,
            String planId,
            Instant currentPeriodStart,
            Instant currentPeriodEnd,
            boolean cancelAtPeriodEnd,
            Instant createdAt,
            Instant updatedAt) {
        this.isNew = isNew;
        this.id = id;
        this.customerId = customerId;
        this.planId = planId;
        this.currentPeriodStart = currentPeriodStart;
        this.currentPeriodEnd = currentPeriodEnd;
        this.cancelAtPeriodEnd = cancelAtPeriodEnd;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return isNew;
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
