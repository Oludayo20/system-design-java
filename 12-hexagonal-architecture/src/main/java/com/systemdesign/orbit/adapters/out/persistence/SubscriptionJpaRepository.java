package com.systemdesign.orbit.adapters.out.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository. Only ever instantiated when the "postgres" Spring profile is
 * active (see config.PostgresPersistenceConfig) — the core never sees this interface.
 */
public interface SubscriptionJpaRepository extends JpaRepository<SubscriptionJpaEntity, String> {

    Optional<SubscriptionJpaEntity> findFirstByCustomerIdOrderByCreatedAtDesc(String customerId);
}
