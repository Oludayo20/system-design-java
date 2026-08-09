package com.systemdesign.faas.runtime;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The actual emulator. Every trigger (HTTP, schedule, queue, file-drop) calls into this SAME
 * manager — proving that triggers are just different front doors onto one runtime, exactly like
 * real Lambda: API Gateway, EventBridge, SQS, and S3 notifications all ultimately call the same
 * invoke API against the same execution-environment pool.
 *
 * <p><b>Java vs. the TypeScript sibling — read this before touching cold-start mechanics.</b> The
 * Node port of this project fakes a "fresh module reload" by evicting a require.cache entry and
 * re-requiring it, which re-runs a module's top-level init code. The JVM has no equivalent: a
 * loaded class cannot be un-loaded and re-initialized on demand (short of a custom classloader
 * per invocation, which would be its own large simulation on top of this one). So cold start here
 * is modeled differently, but just as honestly: on a cold path, the manager constructs a BRAND
 * NEW instance of the function's class via its registered factory. Real cost lives in that
 * constructor (see {@code functions/*}, each of which does real "init work" — building internal
 * state, sleeping briefly) — a warm reuse skips the constructor entirely, so it is genuinely,
 * measurably cheaper, not just labeled differently. On top of that real constructor cost, the
 * manager injects the SAME kind of additional latency a real Lambda cold start pays for
 * container/runtime init that neither port can reproduce for real — an actual, measured
 * {@code Thread.sleep(coldStartLatencyMs)}, not a fake label. Same observable effect (a slower
 * first call, every time), different mechanism, stated plainly.
 *
 * <p>Mechanics reproduced for real (not just logged):
 * <ul>
 *   <li><b>Warm reuse:</b> an idle instance younger than {@code warmTtlMs} is reused with no
 *       extra latency.</li>
 *   <li><b>Cold start:</b> no idle instance exists (or it aged out), so a NEW instance is
 *       constructed (real constructor cost) and a real {@code coldStartLatencyMs} delay is slept
 *       before the handler is allowed to run.</li>
 *   <li><b>Scale-to-zero:</b> a background sweeper evicts idle instances past {@code warmTtlMs}.</li>
 *   <li><b>Concurrency scaling:</b> concurrent invocations that find no idle instance each
 *       construct their own new instance — a burst of N concurrent calls with nothing warm yet
 *       produces N cold starts, same as Lambda scaling out concurrent execution environments.
 *       Claiming a warm instance is a single atomic {@link ConcurrentLinkedDeque#pollFirst()},
 *       so two concurrent invocations can never claim the same idle instance.</li>
 *   <li><b>Billing:</b> real wall-clock handler duration (measured with {@link System#nanoTime()}),
 *       rounded up to the nearest 100ms ("billed duration", AWS's historical Lambda billing
 *       granularity), accumulated per function.</li>
 * </ul>
 */
public class ExecutionEnvironmentManager {

    private static final Logger log = LoggerFactory.getLogger(ExecutionEnvironmentManager.class);

    private final ExecutionEnvironmentManagerOptions options;
    private final Map<String, Supplier<LambdaFunction>> registry = new ConcurrentHashMap<>();
    private final Map<String, ConcurrentLinkedDeque<FunctionInstance>> pools = new ConcurrentHashMap<>();
    private final Map<String, FunctionStats> stats = new ConcurrentHashMap<>();

    private volatile ScheduledExecutorService sweeper;

    public ExecutionEnvironmentManager(ExecutionEnvironmentManagerOptions options) {
        this.options = options;
    }

    /** Register a function name against a factory that constructs a fresh instance per cold start. */
    public void register(String name, Supplier<LambdaFunction> factory) {
        registry.put(name, factory);
        pools.put(name, new ConcurrentLinkedDeque<>());
        stats.put(name, new FunctionStats());
    }

    /** Starts the background sweeper using {@code options.sweepIntervalMs()}. */
    public synchronized void startSweeper() {
        startSweeper(options.sweepIntervalMs());
    }

    public synchronized void startSweeper(long intervalMs) {
        if (sweeper != null) {
            return;
        }
        sweeper = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "faas-sweeper");
            thread.setDaemon(true);
            return thread;
        });
        sweeper.scheduleAtFixedRate(this::sweep, intervalMs, intervalMs, TimeUnit.MILLISECONDS);
    }

    public synchronized void stopSweeper() {
        if (sweeper != null) {
            sweeper.shutdownNow();
            sweeper = null;
        }
    }

    private void sweep() {
        long now = System.currentTimeMillis();
        for (Map.Entry<String, ConcurrentLinkedDeque<FunctionInstance>> entry : pools.entrySet()) {
            String name = entry.getKey();
            entry.getValue().removeIf(instance -> {
                long idleMs = now - instance.lastUsedAt();
                boolean expired = idleMs > options.warmTtlMs();
                if (expired) {
                    log.info("[sweeper] evicted {} instance={} (idle {}ms > TTL {}ms) — scaled to zero",
                            name, instance.id(), idleMs, options.warmTtlMs());
                }
                return expired;
            });
        }
    }

    /**
     * Invokes the named function, claiming a warm instance if one is available or constructing a
     * fresh (cold) one otherwise. Every trigger adapter calls this same method.
     */
    public InvokeResult invoke(String name, LambdaEvent event) {
        Supplier<LambdaFunction> factory = registry.get(name);
        if (factory == null) {
            throw new IllegalArgumentException("No function registered as \"" + name + "\"");
        }

        ConcurrentLinkedDeque<FunctionInstance> pool = pools.get(name);
        FunctionInstance instance = claimWarmInstance(name, pool);
        boolean cold = instance == null;

        if (cold) {
            // Genuine cold-start cost #1: constructing a brand-new instance of the function
            // class. Real init work happens inside the constructor (see functions/*), so this
            // line itself measurably costs more than reusing a warm instance would.
            LambdaFunction handler = factory.get();
            instance = new FunctionInstance(UUID.randomUUID().toString(), handler, System.currentTimeMillis());

            // Genuine cold-start cost #2: the same kind of extra latency a real fresh Lambda
            // execution environment pays for container/runtime init, which this local demo has
            // no real container to boot for — injected as an actual, measured Thread.sleep, not
            // a fake label.
            sleepUninterruptibly(options.coldStartLatencyMs());
        }

        String requestId = UUID.randomUUID().toString();
        LambdaContext context = new LambdaContext(name, requestId, cold, Instant.now());

        long invokeStartNanos = System.nanoTime();
        LambdaResponse response;
        try {
            response = instance.handler().handle(event, context);
        } finally {
            instance.touch(System.currentTimeMillis());
            pool.addFirst(instance);
        }
        long durationMs = (System.nanoTime() - invokeStartNanos) / 1_000_000;

        // Real AWS Lambda billing granularity: round up to the nearest 100ms, minimum 100ms.
        long billedMs = Math.max(100, ceilTo100(durationMs));

        stats.get(name).record(cold, billedMs);

        log.info("[runtime] {} req={} cold={} duration={}ms billed={}ms instance={}",
                name, requestId, cold, durationMs, billedMs, instance.id());

        return new InvokeResult(name, requestId, cold, durationMs, billedMs, instance.id(), response);
    }

    /**
     * Atomically claims an idle, non-expired instance if one exists. {@link ConcurrentLinkedDeque#pollFirst()}
     * is lock-free and linearizable, so two threads racing to invoke the same function can never
     * both claim the same instance — exactly the race the spec calls out.
     */
    private FunctionInstance claimWarmInstance(String name, ConcurrentLinkedDeque<FunctionInstance> pool) {
        long now = System.currentTimeMillis();
        while (true) {
            FunctionInstance candidate = pool.pollFirst();
            if (candidate == null) {
                return null;
            }
            if (now - candidate.lastUsedAt() > options.warmTtlMs()) {
                // Expired but not yet swept — discard it here rather than handing out a stale
                // instance; the next sweep tick would have evicted it anyway.
                log.info("[runtime] discarded expired idle instance name={} instance={}", name, candidate.id());
                continue;
            }
            return candidate;
        }
    }

    public Map<String, FunctionStatsSnapshot> getStats() {
        Map<String, FunctionStatsSnapshot> out = new LinkedHashMap<>();
        for (String name : registry.keySet()) {
            FunctionStats stat = stats.get(name);
            int warmInstances = pools.get(name).size();
            out.put(name, new FunctionStatsSnapshot(
                    stat.invocations(), stat.coldStarts(), stat.warmStarts(), stat.totalBilledMs(), warmInstances));
        }
        return out;
    }

    private static long ceilTo100(long ms) {
        return ((ms + 99) / 100) * 100;
    }

    private static void sleepUninterruptibly(long ms) {
        if (ms <= 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
