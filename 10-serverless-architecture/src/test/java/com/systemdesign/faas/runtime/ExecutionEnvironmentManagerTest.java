package com.systemdesign.faas.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

/**
 * Pure unit tests for {@link ExecutionEnvironmentManager} — no Spring context, no HTTP server, no
 * RabbitMQ required. Mirrors the style of
 * {@code 05-resilience}'s {@code CircuitBreakerTest} and the TypeScript sibling's
 * {@code execution-environment-manager.spec.ts}: real timers, real measured delays, no mocking of
 * the manager's own clock or sleep calls.
 */
class ExecutionEnvironmentManagerTest {

    @Test
    void isColdOnFirstInvocationAndWarmOnAnImmediateSecondOne() {
        ExecutionEnvironmentManager manager = new ExecutionEnvironmentManager(
                new ExecutionEnvironmentManagerOptions(5_000, 20, 5_000));
        manager.register("echo", EchoFunction::new);

        InvokeResult first = manager.invoke("echo", LambdaEvent.of(Map.of("n", 1)));
        assertTrue(first.cold());

        InvokeResult second = manager.invoke("echo", LambdaEvent.of(Map.of("n", 2)));
        assertFalse(second.cold());
        assertEquals(first.instanceId(), second.instanceId());
    }

    @Test
    void injectsARealMeasurableDelayOnColdStart() {
        ExecutionEnvironmentManager manager = new ExecutionEnvironmentManager(
                new ExecutionEnvironmentManagerOptions(5_000, 150, 5_000));
        manager.register("echo", EchoFunction::new);

        long wallClockStart = System.currentTimeMillis();
        InvokeResult result = manager.invoke("echo", LambdaEvent.of(Map.of()));
        long wallClockElapsed = System.currentTimeMillis() - wallClockStart;

        assertTrue(result.cold());
        assertTrue(wallClockElapsed >= 150, "expected elapsed >= 150ms, was " + wallClockElapsed);
    }

    @Test
    void goesColdAgainOnceTheWarmTtlHasExpired() throws InterruptedException {
        ExecutionEnvironmentManager manager = new ExecutionEnvironmentManager(
                new ExecutionEnvironmentManagerOptions(100, 10, 5_000));
        manager.register("echo", EchoFunction::new);

        InvokeResult first = manager.invoke("echo", LambdaEvent.of(Map.of()));
        assertTrue(first.cold());

        Thread.sleep(150);

        InvokeResult second = manager.invoke("echo", LambdaEvent.of(Map.of()));
        assertTrue(second.cold());
    }

    @Test
    void billsIn100msIncrementsAndAccumulatesPerFunctionStats() {
        ExecutionEnvironmentManager manager = new ExecutionEnvironmentManager(
                new ExecutionEnvironmentManagerOptions(5_000, 0, 5_000));
        manager.register("echo", EchoFunction::new);

        InvokeResult result = manager.invoke("echo", LambdaEvent.of(Map.of()));
        assertEquals(0, result.billedMs() % 100);
        assertTrue(result.billedMs() >= 100);

        FunctionStatsSnapshot stats = manager.getStats().get("echo");
        assertEquals(1, stats.invocations());
        assertEquals(1, stats.coldStarts());
        assertEquals(0, stats.warmStarts());
        assertEquals(result.billedMs(), stats.totalBilledMs());
    }

    @Test
    void spinsUpASeparateInstancePerConcurrentInvocationWhenNothingIsIdleYet() {
        ExecutionEnvironmentManager manager = new ExecutionEnvironmentManager(
                new ExecutionEnvironmentManagerOptions(5_000, 50, 5_000));
        manager.register("echo", EchoFunction::new);

        List<CompletableFuture<InvokeResult>> futures = IntStream.range(0, 3)
                .mapToObj(i -> CompletableFuture.supplyAsync(
                        () -> manager.invoke("echo", LambdaEvent.of(Map.of("n", i)))))
                .collect(Collectors.toList());

        List<InvokeResult> results = futures.stream().map(CompletableFuture::join).collect(Collectors.toList());

        Set<String> instanceIds = results.stream().map(InvokeResult::instanceId).collect(Collectors.toSet());
        assertEquals(3, instanceIds.size());
        assertTrue(results.stream().allMatch(InvokeResult::cold));

        FunctionStatsSnapshot stats = manager.getStats().get("echo");
        assertEquals(3, stats.invocations());
        assertEquals(3, stats.coldStarts());
    }

    @Test
    void theSweeperEvictsIdleInstancesPastTtlScalingToZero() throws InterruptedException {
        ExecutionEnvironmentManager manager = new ExecutionEnvironmentManager(
                new ExecutionEnvironmentManagerOptions(50, 5, 20));
        manager.register("echo", EchoFunction::new);

        manager.invoke("echo", LambdaEvent.of(Map.of()));
        assertEquals(1, manager.getStats().get("echo").warmInstances());

        manager.startSweeper();
        Thread.sleep(150);
        manager.stopSweeper();

        assertEquals(0, manager.getStats().get("echo").warmInstances());
    }
}
