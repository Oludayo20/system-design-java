package com.systemdesign.orbit.core.ports.out;

import com.systemdesign.orbit.core.domain.Subscription;
import java.util.Optional;

/**
 * Output port: what the core needs from persistence. It knows nothing about Postgres, JPA, or
 * any other storage technology — just this interface. Two adapters implement it:
 * PostgresSubscriptionRepository and InMemorySubscriptionRepository.
 */
public interface SubscriptionRepositoryPort {

    void save(Subscription subscription);

    Optional<Subscription> findById(String id);

    Optional<Subscription> findByCustomerId(String customerId);
}
