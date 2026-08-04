package com.systemdesign.legacyinmemory.infrastructure;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java port of {@code infrastructure/eventBus.js}'s {@code Topic} class - a Kafka-style topic
 * where a producer publishes an event once and <b>every</b> subscribed consumer group receives
 * its own independent copy of it (broadcast, not competing).
 *
 * <p>doc.md: "Kafka focuses on: Broadcasting events ... Many services can consume the same
 * event independently."
 *
 * <p>See {@link EventQueue} for why a custom class was chosen over
 * {@code ApplicationEventPublisher}/{@code @EventListener}: this class needed to sit next to
 * {@link EventQueue} with a matching API (a {@code Queue}/{@code Topic} pair sharing one file
 * in the original), and needed genuinely concurrent, independent, per-subscriber dispatch with
 * per-subscriber error isolation - each subscriber runs on its own task via a cached thread
 * pool, exactly like the original's {@code Promise.resolve().then(() => sub.handler(...))}
 * "fire and forget, independently, in parallel" dispatch, catching each subscriber's own
 * rejection separately.
 *
 * @param <T> the event payload type
 */
public class Topic<T> {

    private static final AtomicInteger eventCounter = new AtomicInteger(1);

    private final String name;
    private final List<Subscriber<T>> subscribers = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "topic-dispatch");
        t.setDaemon(true);
        return t;
    });

    public Topic(String name) {
        this.name = name;
    }

    public void subscribe(String groupName, ThrowingConsumer<T> handler) {
        subscribers.add(new Subscriber<>(groupName, handler));
        System.out.println("  [Topic:" + name + "] consumer group \"" + groupName + "\" subscribed");
    }

    public int publish(T payload) {
        int eventId = eventCounter.getAndIncrement();
        System.out.println("  [Topic:" + name + "] broadcasting event #" + eventId + " to " + subscribers.size()
                + " independent consumer group(s)");
        for (Subscriber<T> sub : subscribers) {
            // Each subscriber processes the event independently and in parallel.
            executor.submit(() -> {
                try {
                    sub.handler().accept(payload);
                } catch (Exception err) {
                    System.out.println("  [Topic:" + name + "] consumer group \"" + sub.groupName()
                            + "\" errored: " + err.getMessage());
                }
            });
        }
        return eventId;
    }

    private record Subscriber<T>(String groupName, ThrowingConsumer<T> handler) {
    }
}
