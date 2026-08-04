package com.systemdesign.legacyinmemory.infrastructure;

import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Java port of {@code infrastructure/eventBus.js}'s {@code Queue} class - a RabbitMQ-style
 * queue where a producer publishes jobs and a pool of workers <b>competes</b> to pull each job
 * off the queue, so each job is handled exactly once (by whichever worker grabs it first).
 *
 * <p>doc.md: "Producer -&gt; Queue -&gt; Consumer", "Task distribution",
 * "Retrying Failed Jobs" (retry + Dead Letter Queue).
 *
 * <h2>Why a custom class instead of Spring's {@code ApplicationEventPublisher}</h2>
 * Spring's built-in event mechanism dispatches an event to every {@code @EventListener}
 * (broadcast, like {@link Topic}) and by default does so synchronously on the publisher's
 * thread. The original {@code Queue} is a <b>competing-consumer</b> queue with per-job retry
 * counts and a Dead Letter Queue, which has no equivalent in Spring's event system. A small
 * hand-rolled class - one background thread per registered worker, continuously polling a
 * shared job deque - is a much closer behavioral match to the original's
 * {@code while (true) { job = queue.getNextJob(); ... }} worker loop.
 *
 * <p>The original's {@code jobCounter} is a module-level variable shared by every {@code Queue}
 * instance in the process; {@link #jobCounter} reproduces that by being {@code static}.
 *
 * @param <T> the job payload type
 */
public class EventQueue<T> {

    private static final AtomicInteger jobCounter = new AtomicInteger(1);

    private final String name;
    private final ConcurrentLinkedDeque<Job<T>> jobs = new ConcurrentLinkedDeque<>();
    private final List<Job<T>> deadLetterQueue = new CopyOnWriteArrayList<>();
    private final List<Thread> workerThreads = new CopyOnWriteArrayList<>();
    private volatile boolean stopped = false;

    public EventQueue(String name) {
        this.name = name;
    }

    public int publish(T payload) {
        Job<T> job = new Job<>(jobCounter.getAndIncrement(), payload);
        jobs.addLast(job);
        System.out.println("  [Queue:" + name + "] published job #" + job.id + " (queue length: " + jobs.size() + ")");
        return job.id;
    }

    /** Registers a worker with the original's defaults: {@code maxRetries=2, retryDelayMs=200}. */
    public void registerWorker(String workerName, ThrowingConsumer<T> handler) {
        registerWorker(workerName, handler, 2, 200);
    }

    /**
     * Registers a worker that continuously pulls jobs off the queue, mirroring doc.md's
     * {@code while (true) { job = queue.getNextJob(); ... }}. Each registered worker runs on
     * its own daemon thread so multiple workers on the same queue genuinely compete for jobs,
     * matching the original where {@code inventory-worker} and {@code notification-worker} are
     * both registered on the same shared {@code orderQueue} and race for each job.
     */
    public void registerWorker(String workerName, ThrowingConsumer<T> handler, int maxRetries, long retryDelayMs) {
        Thread worker = new Thread(() -> runLoop(workerName, handler, maxRetries, retryDelayMs),
                "queue-" + name + "-" + workerName);
        worker.setDaemon(true);
        workerThreads.add(worker);
        worker.start();
    }

    private void runLoop(String workerName, ThrowingConsumer<T> handler, int maxRetries, long retryDelayMs) {
        while (!stopped) {
            Job<T> job = jobs.pollFirst();
            if (job == null) {
                Delay.sleep(30);
                continue;
            }
            try {
                handler.accept(job.payload);
            } catch (Exception err) {
                job.attempts += 1;
                if (job.attempts <= maxRetries) {
                    System.out.println("  [Queue:" + name + "] worker \"" + workerName + "\" failed job #" + job.id
                            + " (attempt " + job.attempts + "/" + maxRetries + ") - retrying in " + retryDelayMs
                            + "ms: " + err.getMessage());
                    Delay.sleep(retryDelayMs);
                    jobs.addLast(job);
                } else {
                    System.out.println("  [Queue:" + name + "] job #" + job.id
                            + " exhausted retries -> moved to Dead Letter Queue");
                    deadLetterQueue.add(job);
                }
            }
        }
    }

    public boolean isIdle() {
        return jobs.isEmpty();
    }

    public int jobsLength() {
        return jobs.size();
    }

    public List<Job<T>> getDeadLetterQueue() {
        return deadLetterQueue;
    }

    public void stop() {
        stopped = true;
    }

    public static class Job<T> {
        final int id;
        final T payload;
        int attempts = 0;

        Job(int id, T payload) {
            this.id = id;
            this.payload = payload;
        }

        public int getId() {
            return id;
        }

        public T getPayload() {
            return payload;
        }

        public int getAttempts() {
            return attempts;
        }
    }
}
