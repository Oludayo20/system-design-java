package com.systemdesign.legacyinmemory.demo;

import com.systemdesign.legacyinmemory.infrastructure.Delay;
import com.systemdesign.legacyinmemory.infrastructure.EventQueue;
import com.systemdesign.legacyinmemory.infrastructure.ShardRouter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Java port of {@code scripts/demo.js} - a narrated, three-act walkthrough of the concepts in
 * doc.md. Activated with the {@code demo} Spring profile:
 *
 * <pre>mvn spring-boot:run -Dspring-boot.run.profiles=demo</pre>
 *
 * <p><b>Why a {@code CommandLineRunner} instead of a documented curl sequence:</b> the original
 * {@code demo.js} is not just a sequence of requests - it also narrates timing (how long
 * {@code placeOrder} takes to respond vs. how long the background workers keep running after),
 * runs a completely standalone queue/retry/DLQ simulation (Act 2), and a standalone sharding
 * simulation (Act 3) that have no HTTP surface at all. A runnable class reproduces all of that
 * narration and timing faithfully; a curl-only README could not.
 *
 * <p><b>Deviations from the original, and why:</b>
 * <ul>
 *   <li>The original's Act 1 calls {@code bootstrap()} itself, spinning up a brand new,
 *   throwaway {@code db}/{@code cache}/{@code orderQueue}/{@code saleTopic} and a temporary
 *   HTTP server on port 4500, separate from whatever {@code main.js} might also have running.
 *   In this Spring port there is exactly one application context, so Act 1 instead drives the
 *   application's own already-running singleton services over HTTP (via {@link RestTemplate}
 *   against {@code localhost:<server.port>}, which the {@code demo} profile sets to 4500 in
 *   {@code application-demo.yml} to match the original's port). This is the natural
 *   Spring-idiomatic equivalent of "the app under demo, hit over HTTP".</li>
 *   <li>The original calls {@code process.exit(0)} at the end. This runner calls
 *   {@link SpringApplication#exit} followed by {@link System#exit}, the Spring-idiomatic way to
 *   shut the whole process down after a {@code CommandLineRunner} finishes, matching the
 *   original's "run once and exit" behavior.</li>
 * </ul>
 */
@Component
@Profile("demo")
public class DemoRunner implements CommandLineRunner {

    private final ConfigurableApplicationContext context;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${server.port}")
    private int port;

    public DemoRunner(ConfigurableApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(String... args) throws Exception {
        actOneModularMonolith();
        actTwoTrafficSpikeAndRetries();
        actThreeSharding();

        heading("Done - see README.md for how each file maps back to doc.md");

        int exitCode = SpringApplication.exit(context, () -> 0);
        System.exit(exitCode);
    }

    // ---------------------------------------------------------------------
    // Act 1 - Modular Monolith + Async Processing + Caching
    // ---------------------------------------------------------------------
    private void actOneModularMonolith() {
        heading("ACT 1 - Modular Monolith + Async Processing + Caching (doc.md)");
        System.out.println(
                "One repository, one deployment, one process - but internally split into\n"
                        + "Identity / Catalog / Basket / Ordering modules that talk through events,\n"
                        + "not direct function calls into each other's internals.");

        String base = "http://localhost:" + port;
        HttpHeaders jsonHeaders = new HttpHeaders();
        jsonHeaders.setContentType(MediaType.APPLICATION_JSON);

        step("Identity module: register + login");
        restTemplate.postForEntity(base + "/auth/register",
                new HttpEntity<>(Map.of("email", "ada@example.com", "password", "secret123"), jsonHeaders),
                Map.class);
        var loginRes = restTemplate.postForEntity(base + "/auth/login",
                new HttpEntity<>(Map.of("email", "ada@example.com", "password", "secret123"), jsonHeaders),
                Map.class);
        Object token = loginRes.getBody() != null ? loginRes.getBody().get("token") : null;
        System.out.println("[Client] received token: " + token);

        step("Catalog module + Redis-style cache");
        System.out.println("[Client] GET /products/1 (first time - expect a cache MISS)");
        restTemplate.getForEntity(base + "/products/1", String.class);
        System.out.println("[Client] GET /products/1 (again - expect a cache HIT)");
        restTemplate.getForEntity(base + "/products/1", String.class);

        step("Basket module");
        restTemplate.postForEntity(base + "/basket/1/items",
                new HttpEntity<>(Map.of("productId", 1, "qty", 1), jsonHeaders), Map.class);

        step("Ordering module: place order (this is the doc.md \"async\" payoff)");
        long requestStart = System.currentTimeMillis();
        var orderRes = restTemplate.postForEntity(base + "/orders/1", null, Map.class);
        @SuppressWarnings("unchecked")
        Map<String, Object> orderBody = orderRes.getBody() != null
                ? (Map<String, Object>) orderRes.getBody().get("order")
                : Map.<String, Object>of();
        System.out.println("[Client] response received after " + (System.currentTimeMillis() - requestStart)
                + "ms: \"" + orderBody.get("status") + "\" order #" + orderBody.get("id")
                + ", total $" + orderBody.get("total"));
        System.out.println("[Client] the customer's screen can now show \"Order placed!\" ...");
        System.out.println("          ...meanwhile inventory/notification/analytics/fraud-detection are still working:");

        // Give the background workers time to finish so their log lines print before we move
        // on - in a real system the process would just keep running.
        Delay.sleep(1200);
    }

    // ---------------------------------------------------------------------
    // Act 2 - Traffic Spikes & Retrying Failed Jobs
    // ---------------------------------------------------------------------
    private void actTwoTrafficSpikeAndRetries() {
        heading("ACT 2 - Traffic Spikes & Retrying Failed Jobs (doc.md)");
        System.out.println("doc.md: \"Without a queue: your server crashes. With RabbitMQ: jobs are");
        System.out.println("gradually processed by a small pool of workers.\"");

        EventQueue<EmailJob> spikeQueue = new EventQueue<>("flash-sale-emails");
        AtomicInteger processed = new AtomicInteger(0);
        final int totalJobs = 30;
        final int workerCount = 4;

        for (int i = 0; i < workerCount; i++) {
            String workerName = "email-worker-" + (i + 1);
            spikeQueue.registerWorker(workerName, payload -> {
                Delay.sleep(120);
                processed.incrementAndGet();
            });
        }

        step("Producer publishes " + totalJobs + " email jobs almost instantly (flash sale)");
        for (int i = 1; i <= totalJobs; i++) {
            spikeQueue.publish(new EmailJob("user" + i + "@example.com"));
        }

        while (!spikeQueue.isIdle() || processed.get() < totalJobs) {
            Delay.sleep(150);
            System.out.println("  ... " + processed.get() + "/" + totalJobs + " emails sent so far (" + workerCount
                    + " workers, queue length " + spikeQueue.jobsLength() + ")");
        }
        System.out.println("[Result] all " + totalJobs + " jobs processed without the API ever blocking on a single request.");
        spikeQueue.stop();

        step("Retry + Dead Letter Queue");
        EventQueue<SmsJob> flakyQueue = new EventQueue<>("flaky-sms");
        AtomicInteger attempts = new AtomicInteger(0);
        flakyQueue.registerWorker("sms-worker", payload -> {
            int attempt = attempts.incrementAndGet();
            if (payload.willFailForever()) {
                throw new RuntimeException("SMS provider permanently rejecting this number");
            }
            if (attempt < 2) {
                throw new RuntimeException("SMS provider temporarily unavailable");
            }
            System.out.println("  [SMS Worker] delivered SMS to " + payload.to());
        }, 2, 250);

        flakyQueue.publish(new SmsJob("+234-000-0000", false)); // will fail once, then succeed
        flakyQueue.publish(new SmsJob("+234-111-1111", true));  // ends up in the DLQ

        Delay.sleep(2000);
        System.out.println("[Result] Dead Letter Queue now holds " + flakyQueue.getDeadLetterQueue().size()
                + " permanently failed job(s).");
        flakyQueue.stop();
    }

    // ---------------------------------------------------------------------
    // Act 3 - Database Sharding
    // ---------------------------------------------------------------------
    private void actThreeSharding() {
        heading("ACT 3 - Database Sharding (doc.md)");
        System.out.println("doc.md worked example: userId % 3, users 15, 230, 987, 1500.");

        ShardRouter router = new ShardRouter(3, "hash");
        for (int userId : new int[] {15, 230, 987, 1500}) {
            int shard = router.shardIndexFor(userId);
            System.out.println("  User " + userId + " -> Shard " + shard);
        }

        step("Simulating Oja growing to a larger user base (hash-based routing)");
        ShardRouter bigRouter = new ShardRouter(3, "hash");
        List<Integer> sampleUserIds = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            sampleUserIds.add(1000 + i * 137);
        }
        for (int userId : sampleUserIds) {
            bigRouter.writeUser(userId, Map.of("id", userId, "email", "user" + userId + "@oja.example"));
        }
        System.out.println("  Distribution across shards: " + bigRouter.distributionSummary());

        int lookupId = sampleUserIds.get(5);
        ShardRouter.ShardReadResult result = bigRouter.readUser(lookupId);
        System.out.println("  Login lookup for user " + lookupId + " -> only Shard " + result.shardIndex()
                + " is queried (not all shards).");

        step("Why sharding is usually a last resort");
        System.out.println("  Stage 1: single PostgreSQL -> Stage 2: + Redis cache -> Stage 3: + read replicas");
        System.out.println("  Stage 4: partition large tables -> Stage 5: shard (most apps never need this).");
    }

    private void heading(String title) {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println(title);
        System.out.println("=".repeat(70));
    }

    private void step(String title) {
        System.out.println();
        System.out.println("--- " + title + " ---");
    }
}
