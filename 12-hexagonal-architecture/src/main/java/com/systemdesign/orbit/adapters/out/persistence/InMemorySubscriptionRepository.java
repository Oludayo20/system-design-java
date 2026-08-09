package com.systemdesign.orbit.adapters.out.persistence;

import com.systemdesign.orbit.core.domain.Subscription;
import com.systemdesign.orbit.core.ports.out.SubscriptionRepositoryPort;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outbound/driven adapter #1 for SubscriptionRepositoryPort — an in-memory store. Implements the
 * exact same port as PostgresSubscriptionRepository, with no database at all. Selected via
 * {@code app.repository=memory}, and reused directly (via {@code new}, no Spring) by the core's
 * unit tests.
 */
public class InMemorySubscriptionRepository implements SubscriptionRepositoryPort {

    private final Map<String, Subscription> byId = new ConcurrentHashMap<>();

    @Override
    public void save(Subscription subscription) {
        byId.put(subscription.getId(), subscription);
    }

    @Override
    public Optional<Subscription> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<Subscription> findByCustomerId(String customerId) {
        return byId.values().stream()
                .filter(s -> s.getCustomerId().equals(customerId))
                .max(Comparator.comparing(Subscription::getCreatedAt));
    }
}
