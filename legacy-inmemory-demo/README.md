# legacy-inmemory-demo (Java / Spring Boot port)

This is a Java/Spring Boot port of [`system-design/legacy-inmemory-demo`](../../system-design/legacy-inmemory-demo),
the original single-process, in-memory simulation of the concepts in
[`doc.md`](../doc.md): modular monoliths, event-driven communication (RabbitMQ/Kafka style),
Redis-style caching, and database sharding.

**This project predates, and is superseded by, the other four projects in this repo**
(`01-modular-monolith`, `02-database-sharding`, `03-async-queue-processing`,
`04-ecom-marketplace-capstone`), which do the same thing against *real* PostgreSQL, Redis, and
RabbitMQ/Kafka via Docker Compose. It's kept only for reference, exactly like its Node.js
counterpart. There is no database, cache, or message broker here - everything is simulated
in-process with plain Java objects and `ConcurrentHashMap`s, so it needs no infrastructure at
all to run.

## How the original's pieces map to this port

| Original (`legacy-inmemory-demo`, Node.js) | This port (Java/Spring) |
|---|---|
| `src/infrastructure/database.js` (`Database`) | `infrastructure/InMemoryDatabase.java` - `ConcurrentHashMap<String, List<Object>>` "tables", with `insert`/`find` simulating a 20ms round-trip via `Thread.sleep` |
| `src/infrastructure/cache.js` (`Cache`) | `infrastructure/InMemoryCache.java` - `ConcurrentHashMap` with per-entry TTL (default 30s, same as the original) |
| `src/infrastructure/delay.js` (`delay`) | `infrastructure/Delay.java` - blocking `Thread.sleep`, used the same way the original uses `await delay(ms)` |
| `src/infrastructure/eventBus.js` (`Queue`) | `infrastructure/EventQueue.java` - a **custom in-process pub/sub class**, not `ApplicationEventPublisher` (see below) |
| `src/infrastructure/eventBus.js` (`Topic`) | `infrastructure/Topic.java` - same reasoning |
| `src/infrastructure/sharding.js` (`ShardRouter`) | `infrastructure/ShardRouter.java` - identical hash/range routing logic over several `InMemoryDatabase` instances |
| `src/modules/*/*.module.js` | `modules/*/​*Service.java` (`@Service`) |
| `src/modules/inventory/inventory.worker.js`, `notification.worker.js`, `analytics.worker.js` | `modules/{inventory,notification,analytics}/*.java` (`@Component`, register on startup via `@PostConstruct`) |
| `src/api/server.js` (Express routes) | `api/*Controller.java` (Spring MVC `@RestController`) |
| `src/bootstrap.js` | `infrastructure/InfrastructureConfig.java` (`@Configuration` beans) + Spring's own component wiring |
| `src/main.js` | `LegacyInMemoryDemoApplication.java` (default profile: starts the app, prints example `curl` commands) |
| `scripts/demo.js` | `demo/DemoRunner.java` (`@Profile("demo")` `CommandLineRunner` - see "Running the narrated demo" below) |

### Why a custom event bus class instead of Spring's `ApplicationEventPublisher`

The original's `Queue` is a **RabbitMQ-style competing-consumer queue**: multiple workers pull
from one job list, each job is handled exactly once, failed jobs are retried with backoff up to
`maxRetries` and then moved to a Dead Letter Queue. Its `Topic` is a **Kafka-style broadcast**:
every subscriber gets its own independent copy of every event, dispatched in parallel, with each
subscriber's errors isolated from the others.

Spring's `ApplicationEventPublisher`/`@EventListener` only gives you the broadcast half of that
picture (and dispatches synchronously by default), with no notion of competing consumers, retry
counts, or a dead-letter queue. So this port keeps the original's two-class shape almost exactly:
`EventQueue<T>` (one background daemon thread per registered worker, all pulling from a shared
`ConcurrentLinkedDeque`, with the same retry/DLQ semantics) and `Topic<T>` (a cached thread pool
dispatches to every subscriber independently, catching each one's exceptions separately). This
was the closer behavioral match, as instructed.

**A behavior worth calling out, faithfully preserved from the original:** `bootstrap.js`
registers *both* `inventory-worker` and `notification-worker` on the **same** `orderQueue`. Since
`Queue` is competing-consumer, that means for any single order, only *one* of
{inventory, notification} actually processes it - whichever worker's poll loop grabs the job
first - not both. This looks like it may be an oversight in the original (you'd normally want
both a stock-reduction *and* a receipt-email job per order), but the brief for this port was
fidelity to the original's actual runtime behavior, not a "fixed" version of it, so
`InventoryWorker`/`NotificationWorker` register on the same shared `EventQueue<OrderCreatedJob>`
bean here too. `analytics`/`fraud-detection`, by contrast, both subscribe to the same
`Topic<SaleEvent>` and correctly both run on every order, exactly like the original.

### Other notes on the port

- Every domain "table" row (`User`, `Product`, `Cart`/`CartItem`, `Order`) is a small Lombok
  `@Data` class rather than an untyped JS object, but `InMemoryDatabase` still stores rows as
  untyped `Object`s internally (generic `<T>` accessor methods added for callers) - matching the
  original's genuinely untyped, single-shape-per-table storage.
- `CatalogService.seed()` runs via `@PostConstruct` instead of `bootstrap.js`'s explicit
  `await catalog.seed()` call - same one-time-at-startup effect, expressed with Spring's bean
  lifecycle instead of an explicit bootstrap sequence.
- Counters (`nextUserId`, `nextOrderId`, the `Queue`/`Topic` job/event IDs) use `AtomicInteger`
  instead of a plain JS closure variable, since Java handles real concurrent HTTP requests where
  Node's single-threaded event loop didn't need to worry about it.
- No Postgres/Redis/RabbitMQ/Docker - matching the original, which has none either.

## Running it

```bash
mvn spring-boot:run
```

Starts the modular monolith on `http://localhost:3000` (override with `--server.port=<port>` or
`PORT` env var via `SERVER_PORT`) and prints the same example `curl` commands the original's
`main.js` does:

```bash
curl http://localhost:3000/products
curl -X POST http://localhost:3000/auth/register -H "Content-Type: application/json" -d '{"email":"a@b.com","password":"secret"}'
curl -X POST http://localhost:3000/basket/1/items -H "Content-Type: application/json" -d '{"productId":1}'
curl -X POST http://localhost:3000/orders/1
```

Watch the console after placing an order - you'll see `[Ordering]` log the order as saved and
return immediately, then (a little while later, on background threads) exactly one of
`[Inventory Worker]` / `[Notification Worker]`, plus both `[Analytics]` and `[Fraud Detection]`.

### Running the narrated demo (equivalent of `npm run demo`)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=demo
```

This runs `demo/DemoRunner.java`, a faithful port of `scripts/demo.js`'s three acts:

1. **Act 1** - registers/logs in a user, fetches a product twice (cache MISS then HIT), adds it
   to a basket, and places an order, printing how long the HTTP response took to come back
   (fast - it doesn't wait on inventory/notification/analytics/fraud-detection).
2. **Act 2** - a standalone queue simulation: 30 "flash sale" email jobs published almost
   instantly and drained by a 4-worker pool, then a retry + Dead Letter Queue walkthrough (one
   job fails once then succeeds, another fails forever and lands in the DLQ).
3. **Act 3** - a standalone `ShardRouter` simulation: the doc.md worked example
   (`userId % 3` for users 15, 230, 987, 1500), then a bigger simulated user base to show shard
   distribution and single-shard lookup.

The process exits automatically when the demo finishes, matching the original's
`process.exit(0)`.

## Endpoints

| Method | Path | Module |
|---|---|---|
| `POST` | `/auth/register` | Identity |
| `POST` | `/auth/login` | Identity |
| `GET` | `/products` | Catalog |
| `GET` | `/products/:id` | Catalog (cached) |
| `POST` | `/basket/:userId/items` | Basket |
| `GET` | `/basket/:userId` | Basket |
| `POST` | `/orders/:userId` | Ordering |

## Verification note

No Java/Maven toolchain was available in the environment this port was written in, so it could
not be compiled or run to verify. It was written carefully against the Spring Boot 3.3 / Java 21
APIs (`jakarta.*` imports, real Maven Central coordinates) and each file was re-read after
writing as a final correctness pass, but please run `mvn clean verify` after cloning to catch
anything that slipped through.
