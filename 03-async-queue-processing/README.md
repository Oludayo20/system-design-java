# 03 — Asynchronous Processing with Queues (Java / Spring Boot)

A Java/Spring Boot port of the NestJS `03-async-queue-processing` reference project. Same idea:
**don't make users wait for slow work — queue it, and process it after you've already answered
them.** This is how Uber, Amazon, Stripe, Shopify, and Instagram keep their APIs fast while still
doing a lot of work per request.

This project builds it twice, on purpose:

- **RabbitMQ** for the Uber ride example — a job that should be done by exactly *one* worker,
  with a real automatic-retry + dead-letter-queue mechanism.
- **Kafka** for a broadcast comparison — the same event consumed independently by *several*
  services, to make the RabbitMQ-vs-Kafka distinction concrete instead of just a table.

## Producer / Queue / Consumer

- **Producer** — the app creating work. Here, `RidesController`/`RidesService`. Instead of
  synchronously emailing/analyzing/awarding-points, it calls
  `rabbitmq.publish(exchange, routingKey, event)` and returns. It never waits on the side effect,
  only the (cheap) act of handing the job to the broker.
- **Queue** — a waiting line. Like a supermarket checkout: jobs wait, workers pick them up as
  they free up. Here, RabbitMQ's `email.queue` / `analytics.queue` / `loyalty.queue`.
- **Consumer / Worker** — runs in its own **process**, independent of the request/response cycle,
  continuously pulling jobs and processing them. Here, `WorkerApplication`.

## Two processes, one jar — the API/worker split

The original NestJS project has `main.ts` (HTTP API, imports `AppModule`) and a separate
`worker.main.ts` (no HTTP, imports `WorkerModule`) — two independently runnable entrypoints built
from the same `dist/` output, so they can be deployed and scaled independently
(`docker compose up --scale worker=20` never touches the `api` container).

This port reproduces that with **two `@SpringBootApplication` classes in the same Maven module**:

| | `ApiApplication` (`com.systemdesign.asyncqueue.api`) | `WorkerApplication` (`com.systemdesign.asyncqueue.worker`) |
|---|---|---|
| Mirrors | `main.ts` + `app.module.ts` | `worker.main.ts` + `worker.module.ts` |
| Component scan | `.rides`, `.rabbitmq`, `.config` | `.workers`, `.rabbitmq`, `.config` |
| HTTP server | yes (embedded Tomcat) | **no** — started with `WebApplicationType.NONE` |
| Postgres / JPA / Flyway | yes | **no** — `DataSourceAutoConfiguration`, `HibernateJpaAutoConfiguration`, `JpaRepositoriesAutoConfiguration` and `FlywayAutoConfiguration` are explicitly excluded |
| RabbitMQ | publisher only | 3 consumers (email/analytics/loyalty) |

Both halves of the split — **component-scan scoping** (each app only ever sees its own package,
so `WorkerApplication` never even has `Ride`/`RideRepository` on its bean graph, same as
`WorkerModule` never importing `TypeOrmModule`) and **autoconfiguration exclusion** (so the worker
never opens a JDBC connection using Postgres env vars it doesn't have) — are what keep the worker
process's dependency graph free of the database and HTTP server, exactly like the original.

Both apps are packaged into **one** executable jar (`spring-boot-maven-plugin`, `layout:
PropertiesLauncher`). Which class runs is chosen at launch, not at build time:

```bash
java -jar target/async-queue-processing.jar                                    # ApiApplication (default)
LOADER_MAIN=com.systemdesign.asyncqueue.worker.WorkerApplication \
  java -jar target/async-queue-processing.jar                                  # WorkerApplication
```

`docker-compose.yml`'s `api` service leaves `LOADER_MAIN` unset (falls back to the jar's
`Start-Class` manifest entry, `ApiApplication`); the `worker` service sets
`LOADER_MAIN=com.systemdesign.asyncqueue.worker.WorkerApplication` — the direct equivalent of the
original's `command: ['node', 'dist/main.js']` vs `command: ['node', 'dist/worker.main.js']`,
just expressed as an env var instead of a container command because both entrypoints now live
inside a single Spring Boot fat jar rather than two plain `.js` files.

For local dev without Docker:

```bash
mvn spring-boot:run                                                              # API
mvn spring-boot:run -Dspring-boot.run.main-class=com.systemdesign.asyncqueue.worker.WorkerApplication   # worker
```

## The problem, worked through the Uber example

After a ride ends, a naive handler does this, serially, all inside the HTTP request:

| Step                | Time  |
|----------------------|------|
| Save the trip         | 100ms |
| Charge the card       | 500ms |
| Generate PDF receipt  | 2s    |
| Send email             | 3s    |
| Update analytics       | 1s    |
| **Total**              | **~6-7s** |

The rider stares at a spinner for 6-7 seconds. Bad UX, and if the email provider is slow that
day, the whole request hangs with it.

**The async fix:** save the trip, charge the card (still synchronous — the rider needs to know
*now* if their card was declined), then publish one event and return.

`POST /rides` in this repo implements exactly this: it does the DB write and the publish, and
nothing else — see `RidesService.completeRide`.

## RabbitMQ vs Kafka

| RabbitMQ | Kafka |
|---|---|
| Best for background jobs | Best for event streaming |
| Removes messages after processing (typically) | Retains events for replay |
| Queues | Topics |
| Task distribution | Event distribution |
| Emails, notifications | Analytics, activity streams |

Concretely, in this repo:

- **RabbitMQ / `ride_events`** — `ride.completed` is published once and fans out to three
  *queues*; whichever worker replica pulls a given message from `email.queue` is the one and
  only worker that processes it. A job is completed by exactly one consumer.
- **Kafka / `order-events`** — `order.created` is published once and every subscribed *consumer
  group* (`inventory-group`, `analytics-group`, `fraud-group`) gets its own full copy of the
  stream, tracked by its own offset. Adding a fourth consumer group tomorrow doesn't take
  anything away from the other three, and a group can reset its offset to replay history it
  already saw. RabbitMQ queues don't offer that: once a message is acked, it's gone.

## RabbitMQ topology: retry + dead letter queue

Implemented with plain AMQP 0-9-1 features — a topic exchange, a direct exchange, and per-queue
TTL/dead-letter-exchange arguments — not the delayed-message-exchange plugin, so it runs on any
stock RabbitMQ image. Same topology, same names, same timing as the original:

```
producer (POST /rides)
     |
     |  publish "ride.completed"
     v
ride_events  (topic exchange)
     |
     +----------------+----------------+
     v                v                v
email.queue     analytics.queue   loyalty.queue
     |
     |  handler throws
     v
attempt < 3 ?
     |                                  \
    yes                                  no
     |                                    \
     v                                     v
email.queue.retry                  email.queue.dead-letter
(x-message-ttl: 30000ms)           (inspected manually — never auto-drained)
(x-dead-letter-exchange: ride_events.dlx)
(x-dead-letter-routing-key: email.queue)
     |
     |  TTL expires — RabbitMQ auto-redelivers
     v
ride_events.dlx  (direct exchange)
     |  routing key "email.queue"
     v
email.queue   <-- back where it started, attempt count now in the x-retry-count header
```

Mechanics (`com.systemdesign.asyncqueue.rabbitmq.Topology`, `RetryUtil`, `ConsumeWithRetry`):

- Every work queue (`email.queue`, `analytics.queue`, `loyalty.queue`) has a matching `*.retry`
  queue and a matching `*.dead-letter` queue.
- On handler success: **ack**.
- On handler failure: read `x-retry-count` from the message headers, increment it.
  - If the new count is below `MAX_DELIVERY_ATTEMPTS` (3): republish the message to `*.retry`
    with the updated header, ack the original delivery. The retry queue's 30s TTL
    (`RETRY_TTL_MS`) expires, and its `x-dead-letter-exchange` / `x-dead-letter-routing-key`
    arguments cause RabbitMQ to redeliver the message straight back to the main queue — no
    application timer required.
  - If attempts are exhausted: republish to `*.dead-letter` instead, ack the original.
- The retry count lives in a **message header**, not the JSON body, because it's transport
  metadata, not domain data — handlers never need to know about it, and the same
  `ConsumeWithRetry.build(...)` helper works unmodified for all three queues.
- The pure decision — "given this attempt count, do we retry or dead-letter?" — is
  `RetryUtil.shouldDeadLetter(attempt, maxAttempts)`, unit-tested without a broker
  (`RetryUtilTest`).

### Why manual channel-level retry instead of Spring Retry / `@RabbitListener`

This is a deliberate deviation from the "obvious" Spring idiom, made to stay faithful to the
original. `@RabbitListener` + Spring Retry's `RetryOperationsInterceptor` (or
`RepublishMessageRecoverer`) retries **in-process**, immediately, with an optional backoff
computed by the container thread itself — there is no broker-side delay, and a container restart
loses in-flight retry state. The original's retry mechanism is fundamentally different: the delay
is enforced *by RabbitMQ itself*, via a 30-second TTL on a holding queue plus a dead-letter
exchange that redelivers the message once that TTL expires. That is what lets a worker process
restart mid-retry-window without losing anything, and it's what `consume-with-retry.ts` was
built around.

To reproduce that exactly, `ConsumeWithRetry.build(...)` (in `com.systemdesign.asyncqueue.rabbitmq`)
programmatically configures a `SimpleMessageListenerContainer` in `AcknowledgeMode.MANUAL`, with a
`ChannelAwareMessageListener` that:

1. Deserializes the payload and calls the handler.
2. On success, `channel.basicAck(...)`.
3. On failure, computes the next attempt via `RetryUtil`, republishes the raw message bytes (with
   updated headers) to either `*.retry` or `*.dead-letter` via `RabbitmqService.sendToQueue`, and
   **then** acks the original delivery — never a nack-requeue, for the same reason the original
   never uses one: requeueing onto the same queue would retry instantly, with no delay and no
   attempt cap.

Each worker (`EmailWorker`, `AnalyticsWorker`, `LoyaltyWorker`) implements Spring's
`SmartLifecycle` to start/stop this container — the direct analogue of Nest's
`OnModuleInit`/`OnModuleDestroy` hooks in the original workers.

The **Email worker** randomly fails a configurable fraction of jobs (`EMAIL_FAILURE_RATE`,
default `0.3`) specifically so this path is exercisable without manually killing anything — see
[Watching retry + DLQ in action](#watching-retry--dlq-in-action).

## Kafka broadcast comparison

`com.systemdesign.asyncqueue.kafka` mirrors `src/kafka/*.ts` — standalone, DI-free scripts (plain
`main()` methods, never scanned by `ApiApplication` or `WorkerApplication`), matching the
original's explicit "deliberately outside Nest's DI container" design:

- `OrderEventsProducerApp` — builds a Spring Kafka `KafkaTemplate` by hand (no
  `ApplicationContext`) and publishes `order.created` events to the `order-events` topic.
- `OrderEventsConsumerApp` — builds a Spring Kafka `KafkaMessageListenerContainer` by hand
  (also a plain POJO, usable without a Spring context) parameterized by the `GROUP_ID` env var,
  and blocks forever logging every event it receives.

**Why `spring-kafka` classes instead of raw `kafka-clients`:** this project's convention is to use
Spring Kafka for the Kafka pieces; `KafkaTemplate` and `KafkaMessageListenerContainer` are both
plain, directly-instantiable classes that don't require a `SpringApplication`/`ApplicationContext`
to function, so they let these stay one-shot scripts (like the original's `ts-node` invocations)
while still being "Spring Kafka" rather than the lower-level client API.

Run them via `exec-maven-plugin` (added to `pom.xml` for exactly this, not bound to any build
phase):

```bash
mvn compile exec:java -Dexec.mainClass=com.systemdesign.asyncqueue.kafka.OrderEventsConsumerApp \
  -Dexec.args="" -Denv.GROUP_ID=inventory-group -Denv.CONSUMER_LABEL=inventory-worker
```

(`exec:java` doesn't forward `-D` system properties as env vars automatically — simplest is to
export them first: `GROUP_ID=inventory-group CONSUMER_LABEL=inventory-worker mvn compile exec:java
-Dexec.mainClass=com.systemdesign.asyncqueue.kafka.OrderEventsConsumerApp`.)

### Kafka replay

Because Kafka retains events (rather than deleting them once read, like a RabbitMQ queue does), a
consumer group can be rewound to reprocess history — e.g. after fixing a bug in
`analytics-group`'s handler:

```bash
kafka-consumer-groups.sh --bootstrap-server localhost:9092 \
  --group analytics-group --topic order-events --reset-offsets --to-earliest --execute
```

(Or programmatically via Kafka's `Admin` client:
`admin.alterConsumerGroupOffsets("analytics-group", Map.of(partition, new OffsetAndMetadata(0)))`.)
The next time that group's consumer runs, it re-reads every retained `order.created` event from
the beginning — something a RabbitMQ queue, which discards messages once acked, cannot do.

## AWS Lambda as an alternative to a long-running worker

This repo runs workers as an always-on process (`WorkerApplication`) that you scale with
`--scale worker=N`. An alternative architecture skips owning that process entirely:

```
Queue (e.g. SQS, or RabbitMQ via an event-source adapter) -> AWS Lambda -> runs the handler -> stops
```

Trade-offs versus a long-running worker fleet:

- **Cost model** — Lambda bills per invocation/duration; you pay nothing while idle. A worker
  fleet runs (and costs money) 24/7 regardless of queue depth.
- **Scaling** — Lambda scales concurrency automatically per message volume, no `--scale` command
  needed; a self-managed fleet needs you (or an autoscaler watching queue depth) to decide replica
  count.
- **Cold starts / connection reuse** — a long-running worker keeps a warm RabbitMQ/DB connection
  across jobs (here, the `SimpleMessageListenerContainer`'s long-lived channel); Lambda
  invocations pay a cold-start cost and typically can't hold a persistent AMQP channel open the
  way `ConsumeWithRetry` does, since RabbitMQ consumption is a long-lived subscription, not a
  per-message poll (this pattern fits SQS/Kafka better than RabbitMQ for that reason).
- **Operational surface** — no servers/containers to patch or right-size for Lambda; in exchange
  you give up direct control over runtime, retry semantics, and concurrency limits are
  provider-defined.

For this repo's RabbitMQ topology specifically, a Lambda-based worker would most naturally replace
`WorkerApplication` with a Lambda function subscribed via SQS (with RabbitMQ's messages bridged
over, or by using SQS/EventBridge in place of RabbitMQ altogether) — a straightforward swap of the
*consumer* half of the architecture without touching the producer.

## Traffic spikes

Flash sale: 100 emails/min becomes 100,000/min. Without a queue, the API tries to send them all
synchronously and falls over. With this architecture: `POST /rides` still does exactly one INSERT
and one publish — O(1) work regardless of how backed up the workers are — so 100,000 jobs simply
queue up in `email.queue`, and however many workers you're running (1, or 20 via
`--scale worker=20`) drain it as fast as they can. The API's latency doesn't move; only the *queue
depth* (visible in the RabbitMQ management UI) and the time-to-drain change.

See [Traffic-spike demo](#traffic-spike-demo-scale-workers) below to reproduce this.

## Project layout

```
src/main/java/com/systemdesign/asyncqueue/
  api/ApiApplication.java          Producer entrypoint — HTTP API, scans .rides/.rabbitmq/.config
  worker/WorkerApplication.java     Consumer entrypoint — no HTTP, no DB, scans .workers/.rabbitmq/.config
  config/                            OpenAPI metadata
  rabbitmq/                          Topology, retry/DLQ decision logic, RabbitMQ service, consumer helper
  rides/                             POST /rides — producer (entity, DTO, service, controller)
  workers/                           Email / Analytics / Loyalty workers (consumers)
  kafka/                             Standalone Kafka broadcast-comparison scripts
src/main/resources/
  application.yml                    Env-driven configuration
  db/migration/                       Flyway migration for the `rides` table
scripts/
  load-test.sh                       Concurrent POST /rides fire, for the traffic-spike demo
```

## Running it

> **Hosting & deployment:** See [HOSTING.md](./HOSTING.md) for Docker setup, platforms (free → paid), production tooling, and per-component checklists. **API docs:** Swagger UI at `/docs` (springdoc-openapi).

### Prerequisites

Docker (Postgres, RabbitMQ, Kafka) + Java 21 / Maven for `mvn package`/`mvn test` locally.

### 1. Build and validate (no Docker needed for this part)

```bash
mvn clean package
mvn test
```

`mvn package` produces one jar with two independently-runnable entrypoints (`ApiApplication`,
`WorkerApplication` — see [Two processes, one jar](#two-processes-one-jar--the-apiworker-split)).
`mvn test` runs the pure-logic unit tests (retry/DLQ decision logic in `RetryUtilTest`, and that
`RidesService` publishes and returns without any worker dependency in `RidesServiceTest`) — no
broker or database required.

### 2. Start infrastructure

```bash
cp .env.example .env
docker compose up -d
```

This starts Postgres, RabbitMQ (management UI at http://localhost:15672, default
`async_demo` / `async_demo_password`), Kafka (KRaft mode, single broker, on `localhost:9092`),
the `api` service on port 3000, and one `worker` replica.

### 3. Or run the API/worker locally instead of in Docker

```bash
mvn spring-boot:run                                                                                      # API
mvn spring-boot:run -Dspring-boot.run.main-class=com.systemdesign.asyncqueue.worker.WorkerApplication     # worker, in a second terminal
```

(Point `.env`/`application.yml` at `localhost` for Postgres/RabbitMQ if you're running
`docker compose up -d postgres rabbitmq` but the app itself outside Docker.)

### 4. Call the producer

```bash
curl -s -X POST http://localhost:3000/rides \
  -H 'Content-Type: application/json' \
  -d '{
    "riderId": "rider-42",
    "driverId": "driver-7",
    "fare": 24.50,
    "pickupLocation": "Ikeja, Lagos",
    "dropoffLocation": "Lekki, Lagos"
  }' | jq
```

```json
{ "success": true, "rideId": "5f1b6b2e-..." }
```

That response comes back in well under a second — Swagger UI is at http://localhost:3000/docs.

### 5. Watch the workers

```bash
docker compose logs -f worker
```

You'll see the Email, Analytics, and Loyalty workers log independently, each after the HTTP
response above already returned:

```
Receipt emailed to rider rider-42 for ride 5f1b6b2e-... (fare $24.50, Ikeja, Lagos -> Lekki, Lagos)
Recorded sale analytics for ride 5f1b6b2e-...: fare $24.50
Awarded 25 loyalty points to rider rider-42 for ride 5f1b6b2e-...
```

### Watching retry + DLQ in action

`EMAIL_FAILURE_RATE` (default `0.3`) makes the Email worker randomly throw on ~30% of jobs.
POST a handful of rides and watch `docker compose logs -f worker`:

```
email.queue: attempt 1/3 failed (Simulated email provider outage ...) — retrying via email.queue.retry in 30000ms
...30s later, redelivered automatically by the TTL + DLX...
Receipt emailed to rider rider-42 for ride 5f1b6b2e-...
```

To force a job all the way to the dead letter queue, set a higher failure rate before starting the
workers, e.g. `EMAIL_FAILURE_RATE=1` in `.env`, then `docker compose up -d worker`. After 3 failed
attempts you'll see:

```
email.queue: attempt 3/3 failed (...) — routing to email.queue.dead-letter
```

Inspect `email.queue.dead-letter` in the RabbitMQ management UI
(http://localhost:15672/#/queues) — messages sit there for manual inspection and never
auto-retry again.

### Traffic-spike demo: scale workers

```bash
docker compose up -d --scale worker=1
./scripts/load-test.sh           # LOAD_TEST_REQUESTS=500 LOAD_TEST_CONCURRENCY=500 by default
```

Note the reported p50/p95/p99 for `POST /rides` and how long `email.queue` takes to drain (watch
it in the management UI). Then:

```bash
docker compose up -d --scale worker=20
./scripts/load-test.sh
```

The **API's** request latency stays flat in both runs — it only ever does an INSERT and a
publish. What changes is how fast the (now 100,000-job-deep, in a real flash sale) queue drains,
because you added consumers, not because the producer got faster.

> `scripts/load-test.sh` is a bash + curl port of the original's `scripts/load-test.ts` (see
> [Kafka broadcast comparison](#kafka-broadcast-comparison) for why bash rather than a JVM script
> here too: no build step needed to fire an ad-hoc load test, and `curl -w '%{time_total}'` gives
> per-request latency natively). One precision difference: total run duration is measured to the
> second (`date +%s`), not the millisecond, since portable sub-second wall-clock arithmetic in
> POSIX shell isn't worth the complexity for a demo script; per-request latency (used for the
> p50/p95/p99) is unaffected and comes straight from curl at sub-millisecond precision.

### Kafka broadcast demo

Independent of the RabbitMQ flow above — run these from the host (needs `KAFKA_BROKERS` in
`.env`, defaults to `localhost:9092`, which `docker compose up -d kafka` exposes):

```bash
GROUP_ID=inventory-group CONSUMER_LABEL=inventory-worker \
  mvn compile exec:java -Dexec.mainClass=com.systemdesign.asyncqueue.kafka.OrderEventsConsumerApp   # terminal 1
GROUP_ID=analytics-group CONSUMER_LABEL=analytics-worker \
  mvn compile exec:java -Dexec.mainClass=com.systemdesign.asyncqueue.kafka.OrderEventsConsumerApp   # terminal 2
GROUP_ID=fraud-group CONSUMER_LABEL=fraud-worker \
  mvn compile exec:java -Dexec.mainClass=com.systemdesign.asyncqueue.kafka.OrderEventsConsumerApp   # terminal 3
mvn compile exec:java -Dexec.mainClass=com.systemdesign.asyncqueue.kafka.OrderEventsProducerApp     # terminal 4 — publishes 10 order.created events
```

All three consumers log every event:

```
[inventory-worker] received order.created orderId=... partition=0 offset=7 — processed independently of the other consumer groups
[analytics-worker] received order.created orderId=... partition=0 offset=7 — processed independently of the other consumer groups
[fraud-worker] received order.created orderId=... partition=0 offset=7 — processed independently of the other consumer groups
```

Same event, three independent consumer groups, three independent offsets — contrast with
RabbitMQ above, where a `ride.completed` job going to a worker removes it from that queue for
everyone else.

## Tests

```bash
mvn test
```

- `RidesServiceTest` — proves `RidesService.completeRide` saves the ride, publishes
  `ride.completed` to the `ride_events` exchange with the ride payload, and returns
  `{ success: true, rideId }` — using a mocked `RabbitmqService`, so no broker is needed and no
  worker code is anywhere on its dependency graph.
- `RetryUtilTest` — proves `shouldDeadLetter`/`nextAttempt`, the pure functions driving the
  retry-vs-dead-letter decision, without needing a live RabbitMQ connection.

## Deviations from the original

- **`fare` representation** — TypeORM's `@Column('decimal')` maps to a `string` on the JS entity
  (to avoid float rounding on read-back), so the original's `Ride.fare` field is typed `string`.
  The Java port uses `BigDecimal` for the JPA entity (Hibernate's idiomatic mapping for
  `NUMERIC(10,2)`) and formats to a fixed 2-decimal string only when building the
  `RideCompletedEvent` payload — the wire format every consumer actually sees is unchanged.
- **Schema ownership** — the original sets TypeORM's `synchronize: true` even in its "production"
  `docker-compose.yml` (a demo-only shortcut, called out in its own `configuration.ts` comment).
  This port uses a Flyway migration (`V1__create_rides_table.sql`) instead, which is what that
  comment says a real system should do — functionally equivalent schema, more production-shaped
  mechanism.
- **Retry mechanism implementation** — see
  [Why manual channel-level retry instead of Spring Retry](#why-manual-channel-level-retry-instead-of-spring-retry--rabbitlistener)
  above: same wire-level topology and timing, deliberately not using `@RabbitListener` + Spring
  Retry because that would change the retry semantics (in-process vs broker-enforced delay).
- **Kafka scripts** — use Spring Kafka's `KafkaTemplate`/`KafkaMessageListenerContainer`
  instantiated directly rather than through a Spring context, and `kafka-clients`' `AdminClient`
  for the replay example, in place of kafkajs's equivalents. Functionally identical.
- **Load test script** — bash/curl instead of a Node/ts-node script; see the
  [traffic-spike demo](#traffic-spike-demo-scale-workers) section for the one precision
  difference (total-duration measured to the second, not millisecond).

## Not independently verified

No Java/Maven/Docker toolchain was available while writing this port, so `mvn clean verify` and
`docker compose up` have not actually been run against this code. It was written carefully against
Spring Boot 3.3 / Spring AMQP / Spring Kafka / Spring Data JPA / Flyway 10 APIs, but run
`mvn clean verify` after cloning to catch any compilation issues before relying on it — in
particular the `PropertiesLauncher` + `LOADER_MAIN` mechanism for switching entrypoints, and the
`SimpleMessageListenerContainer`/`ChannelAwareMessageListener` retry wiring, are the two most
structurally novel pieces (i.e. least like routine Spring Boot CRUD code) and are worth checking
first.
