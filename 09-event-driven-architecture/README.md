# 09 — Event-Driven Architecture ("FreshCart") — Java / Spring Boot

A Java/Spring Boot port of the NestJS `09-event-driven-architecture` reference project. Same
idea: **a service publishes a fact that already happened, and has no idea — and no need to
know — who's listening.** That's the difference between calling a function and publishing an
event. This project is FreshCart, a grocery delivery app: placing an order fans out to four
completely independent Spring Boot apps, none of which `order-api` imports, calls, or is even
aware exists.

## This is NOT `03-async-queue-processing` again

`03-async-queue-processing` already lives in this repo and already covers "don't make the user
wait — queue it." Read that project first if you haven't; this one deliberately does something
different, on the same broker (RabbitMQ), so the contrast is concrete instead of a table you
skim past:

| | `03-async-queue-processing` | `09-event-driven-architecture` (this project) |
|---|---|---|
| **Pattern** | Point-to-point task queueing | Pub/sub fan-out |
| **Producer→consumer shape** | One producer, one logical consumer *per queue* (`email.queue` has one job of work, done by exactly one `WorkerApplication` replica) | One producer (`order-api`), **many independent consumer apps**, each getting its **own full copy** of every event |
| **Adding a new consumer** | Means adding a new queue the producer's topology also has to know about (see `Topology.java` in `03`, which asserts `email.queue`/`analytics.queue`/`loyalty.queue` by name) | Means binding a brand-new queue to an exchange that already exists — **zero changes to `order-api`**, proven in this repo by `loyalty-consumer` (see below) |
| **A message consumed by worker A** | Is gone — worker B on the same queue will never see it (scale-out = split the work) | Is irrelevant to whether worker B sees it — each consumer app has its **own queue**, so four consumers each see 100% of events (scale-out = replicate the reaction, not split it) |
| **Failure handling shown** | Retry + dead-letter queue (TTL/DLX topology, `ConsumeWithRetry`) | Idempotent processing of a **redelivered duplicate** (a different failure mode: not "it failed," but "it succeeded twice") |
| **What "ordering" means here** | Retry attempts must be ordered after the original delivery | The event must never be visible before its cause (the DB write) is durable — publish-after-commit |
| **RabbitMQ wiring style** | Manual `Channel`/`ChannelAwareMessageListener` (`ConsumeWithRetry`), because the retry semantics need broker-side TTL+DLX, which `@RabbitListener` doesn't give you | Plain Spring AMQP `@RabbitListener` + declarative `TopicExchange`/`Queue`/`Binding` `@Bean`s — no custom retry topology needed |

Both projects queue things. `03` is "one job, done once, by one worker." `09` is "one fact,
broadcast to everyone who cares." If you only remember one line: **queues distribute work,
pub/sub distributes information.**

## Concept, in my own words

An event is a **fact stated in the past tense**: `order.placed`, not `place order`. By the time
anyone downstream sees it, it already happened — there's nothing to negotiate, approve, or
reject about it, only something to react to. That's what makes it safe to publish once and walk
away.

The publisher (`order-api`) sends the event to a broker and is done. It doesn't hold a list of
subscribers, doesn't call anyone's HTTP endpoint, doesn't know if zero services or forty are
listening. Every consumer independently decides what it cares about by declaring its own
`Queue`/`Binding` beans — subscribing is something the *consumer* does, not something the
*producer* grants. That's loose coupling: `loyalty-consumer` in this repo was added after
everything else already existed, and not one line changed in `order-api`, `inventory-consumer`,
`notification-consumer`, or `analytics-consumer` to make room for it.

Because publishing is just "hand the broker a fact and return," the producer's request latency
stops being the sum of everyone who might care. `POST /orders` responds as soon as the order is
saved and the event is handed off — not after stock is decremented, a push notification is sent,
analytics are recorded, and loyalty points are calculated. Those all still happen, just not on
the customer's clock.

None of this is free. Spreading one operation across five independent processes means:

- **Tracing is harder.** A single "place an order" business event now has causally-related
  effects in five different logs, five different JVMs, at four different times. There's no call
  stack connecting them — only a shared `orderId`/`eventId` you have to think to grep for.
- **Ordering isn't automatic.** Nothing about "publish an event" guarantees anyone processes it
  in any particular order relative to other events, or even relative to the write that produced
  it — you have to engineer that guarantee where it matters (see "Ordering" below).
- **Duplicates are not an edge case, they're the contract.** Brokers offering at-least-once
  delivery — which is what makes them reliable in the first place — can and will redeliver a
  message a consumer already successfully processed. "Might receive the same event twice" isn't
  a bug to route around; it's how the guarantee is built. Consumers have to be idempotent (see
  "Idempotency" below).

## Fan-out topology

```
                         POST /orders
                              │
                              ▼
                        ┌───────────┐        commit         ┌──────────┐
                        │ order-api │ ─────────────────────► │ order-db │
                        └───────────┘                        └──────────┘
                              │
                              │ publish "order.placed"
                              │ (only after the commit above resolves)
                              ▼
                  ┌─────────────────────────┐
                  │   grocery_events         │   topic exchange
                  │   (routing key:          │
                  │    order.placed)         │
                  └─────────────────────────┘
                    │        │        │        │
       ┌────────────┘        │        │        └────────────┐
       ▼                     ▼        ▼                     ▼
┌─────────────────┐ ┌──────────────────┐ ┌────────────────┐ ┌─────────────────┐
│ inventory.       │ │ notification.    │ │ analytics.     │ │ loyalty.         │
│ order-placed.    │ │ order-placed.    │ │ order-placed.  │ │ order-placed.    │
│ queue            │ │ queue            │ │ queue          │ │ queue            │
└─────────────────┘ └──────────────────┘ └────────────────┘ └─────────────────┘
       │                     │                    │                    │
       ▼                     ▼                    ▼                    ▼
┌─────────────────┐ ┌──────────────────┐ ┌────────────────┐ ┌─────────────────┐
│ inventory-       │ │ notification-    │ │ analytics-     │ │ loyalty-         │
│ consumer         │ │ consumer         │ │ consumer       │ │ consumer         │
│ decrements stock │ │ logs a push      │ │ increments     │ │ awards points,   │
│ in inventory-db  │ │ notification     │ │ today's sales  │ │ idempotently     │
│ :4101 GET /stock │ │ :4102 GET        │ │ :4103 GET      │ │ :4104 GET        │
│                  │ │ /notifications   │ │ /stats         │ │ /points          │
└─────────────────┘ └──────────────────┘ └────────────────┘ └─────────────────┘
```

Four separate queues bound to one **topic** exchange (not fanout) — a topic exchange lets this
grow to more event types later (`order.cancelled`, `order.refunded`, ...) with each consumer
choosing exactly which routing keys it wants, without every consumer being forced to receive
every event type the way a true fanout exchange would. Today every consumer here only binds
`order.placed`, so it behaves identically to a fanout exchange in practice — the choice is about
where the ceiling is, not what happens on day one.

## Events and consumers

| Event | Published by | Routing key | Consumers |
|---|---|---|---|
| `order.placed` | `order-api` | `order.placed` | `inventory-consumer`, `notification-consumer`, `analytics-consumer`, `loyalty-consumer` |

Every message on `grocery_events` carries the same envelope:

```json
{
  "eventId": "b3f1c2a0-...-uuid",
  "eventType": "order.placed",
  "occurredAt": "2026-08-09T12:00:00Z",
  "payload": {
    "orderId": "5f1b6b2e-...-uuid",
    "customerId": "customer-42",
    "items": [{ "sku": "milk-1l", "name": "Whole Milk 1L", "quantity": 2, "unitPrice": 1.5 }],
    "totalAmount": 3.0
  }
}
```

`eventId` is generated once, when the event is created (`OrderService.toEvent`, via
`UUID.randomUUID()`), and never regenerated on redelivery — it's the field idempotency keys off
(see below).

This record (`OrderPlacedEvent`, a Java `record` with nested `Payload`/`Item` records) is
deliberately **duplicated**, not shared via a common library, in all five Maven modules — see
"Project layout" below for why, and why that's safe with Spring AMQP's default message-conversion
settings.

| App | Role | Port | Inspect with |
|---|---|---|---|
| `order-api` | Producer — the only HTTP write path | `3009` (Swagger `/docs`) | `GET /orders/{id}` |
| `inventory-consumer` | Decrements stock in its own `inventory-db` | `4101` | `GET /stock` |
| `notification-consumer` | Logs/stores a push notification (in-memory) | `4102` | `GET /notifications` |
| `analytics-consumer` | Increments running sales counters (in-memory) | `4103` | `GET /stats` |
| `loyalty-consumer` | Awards loyalty points, idempotently (in-memory) | `4104` | `GET /points` |

## Ordering

There are two different things "ordering" could mean here, and FreshCart only needs one of them.

**Cross-consumer ordering — not needed.** `inventory-consumer` decrementing stock does not, and
should not, depend on `notification-consumer` succeeding, or on any particular order relative to
`analytics-consumer`/`loyalty-consumer`. They're four independent reactions to the same fact, not
four steps in a pipeline. If `notification-consumer`'s process is down for five minutes, stock
still gets decremented immediately and correctly — there is no shared state, lock, or "wait for
the notification to send first" anywhere in this system. This is different from, say, a
`PaymentSuccessful`-before-`WalletCredited` scenario, where processing B before A would be wrong
*because B is causally dependent on A's outcome*. Nothing downstream of `order.placed` here is
causally dependent on another *consumer's* output — only on the order itself existing.

**Publish-after-commit — needed, and implemented for real.** The one ordering guarantee that does
matter: `order.placed` must never become visible to a consumer before the order row it describes
is durably committed. If we published inside the transaction (or before starting it), a consumer
could react to an order that a concurrent `GET /orders/{id}` — or a read replica, or a retried
transaction that later rolls back — can't actually see.
`OrderService.placeOrder` (`order-api/src/main/java/com/systemdesign/freshcart/orderapi/orders/OrderService.java`)
enforces this by construction:

```java
public OrderResponseDto placeOrder(CreateOrderDto dto) {
    Order order = saveOrder(dto);
    // <-- transactionTemplate.execute() above has already committed — everything below only
    // runs because that call returned successfully.

    OrderPlacedEvent event = toEvent(order);
    rabbitTemplate.convertAndSend(GROCERY_EVENTS_EXCHANGE, ORDER_PLACED_ROUTING_KEY, event, ...);
    ...
}

private Order saveOrder(CreateOrderDto dto) {
    return transactionTemplate.execute(status -> {
        ... // build the Order + OrderItems, orders.save(order)
    });
}
```

`saveOrder` runs entirely inside a `TransactionTemplate.execute(...)` callback — a deliberate
choice over annotating `placeOrder` itself `@Transactional`. If the whole method carried
`@Transactional`, the publish call would execute **inside** that same open transaction (Spring
only commits when the annotated method returns), which is exactly the publish-before-commit bug
this design avoids. `TransactionTemplate#execute` returns only once its callback's transaction has
actually been committed (or rolled back) — so `rabbitTemplate.convertAndSend(...)` is textually
*and* temporally after commit, never nested inside it. There is no way for the event to reach
RabbitMQ before PostgreSQL has durably committed the row.

## Idempotency

RabbitMQ (like any broker offering at-least-once delivery) can and does redeliver a message a
consumer already successfully processed — the consumer might crash after doing the work but
before acking, or the broker might decide a delivery needs retrying after a network blip. "The
same event twice" is not a bug to code around; it's the normal cost of the reliability guarantee.
Three of the four consumers here (`inventory-consumer`, `notification-consumer`,
`analytics-consumer`) don't defend against it, on purpose — the point of this project is to
implement the idempotency check *once*, concretely, in the one consumer where a duplicate would
be visibly, financially wrong: `loyalty-consumer`.

`loyalty-consumer`'s `PointsService`
(`loyalty-consumer/src/main/java/com/systemdesign/freshcart/loyaltyconsumer/points/PointsService.java`)
tracks every `eventId` it has already applied — an in-memory `Set<UUID>` for this demo; a real
system would put a unique constraint on `event_id` in whichever table records the side effect. On
each delivery:

```java
public synchronized void awardForOrder(OrderPlacedEvent event) {
    if (processedEventIds.contains(event.eventId())) {
        log.warn("Duplicate delivery of eventId={} (orderId={}) — already processed. "
                + "Skipping so points are not awarded twice.",
                event.eventId(), event.payload().orderId());
        return;
    }
    // ...award points, then:
    processedEventIds.add(event.eventId());
}
```

### Proving it

`loyalty-consumer`'s `SimulateDuplicateDeliveryApp`
(`loyalty-consumer/src/main/java/com/systemdesign/freshcart/loyaltyconsumer/scripts/SimulateDuplicateDeliveryApp.java`)
sends **the same `eventId`, twice**, directly to `loyalty.order-placed.queue` — the same thing a
real RabbitMQ redelivery looks like on the wire. It's a standalone `main()` using
`com.rabbitmq.client` directly (no Spring context), run via the `exec-maven-plugin` — the same
convention `03-async-queue-processing` uses for its standalone Kafka scripts. It targets the queue
directly rather than the `grocery_events` exchange, so the duplicate doesn't also fan out to the
other three consumers, which aren't the ones being tested here and don't have a dedup guard.

```bash
cd loyalty-consumer
mvn compile exec:java -Dexec.mainClass=com.systemdesign.freshcart.loyaltyconsumer.scripts.SimulateDuplicateDeliveryApp
```

Expected output:

```
Simulating a duplicate delivery of eventId=<uuid> to loyalty.order-placed.queue
Expect: exactly one award of 42 points to demo-customer-idempotency

Sending delivery #1...
Sending delivery #2 (identical eventId — simulates redelivery)...

Done. Check: curl http://localhost:4104/points
demo-customer-idempotency should show 42 points (not 84), and processedEventCount should have
increased by exactly 1, not 2.
```

```bash
curl -s http://localhost:4104/points | jq
```

```json
{
  "customers": [{ "customerId": "demo-customer-idempotency", "points": 42 }],
  "processedEventCount": 1
}
```

`loyalty-consumer`'s logs (`docker compose logs loyalty-consumer`) show the second delivery being
explicitly recognized and skipped:

```
order.placed (orderId=..., eventId=b7e4...): awarded 42 points to demo-customer-idempotency (total now 42)
Duplicate delivery of eventId=b7e4... (orderId=...) — already processed. Skipping so points are not awarded twice.
```

This was run for real against a live RabbitMQ instance as part of validating this project — see
[Validation results](#validation-results-from-building-this-project) at the bottom of this file.

## Adding `loyalty-consumer` on day 2 — the actual point of this project

`loyalty-consumer` was written and wired up **after** `order-api`, `inventory-consumer`,
`notification-consumer`, and `analytics-consumer` already existed, in production, handling real
traffic. Getting it live required:

- Writing `loyalty-consumer` as a new Maven module (own `pom.xml`, own `Dockerfile`, own JVM
  process).
- Declaring a new `Queue`/`Binding` bean pair (`loyalty.order-placed.queue`, bound to
  `order.placed`) against the `grocery_events` exchange, which `order-api` had already created
  and been publishing to for however long.
- Adding one service block to `docker-compose.yml`.

It did **not** require:

- Touching a single line of `order-api/src/main/java/**`.
- Redeploying `order-api`.
- Touching `inventory-consumer`, `notification-consumer`, or `analytics-consumer` at all.
- Any coordination beyond knowing the exchange name and the event's JSON shape (documented above)
  — both of which are already implicitly public just by `order-api` having shipped.

That's the concrete version of "loose coupling," not just the phrase.

## The radio station analogy

Think of `order-api` as a radio station and `grocery_events` as the airwaves. The station
broadcasts its show and does not know — cannot know — who owns a radio and has it tuned to that
frequency. It doesn't call each listener to check they're ready. It doesn't wait for anyone to
finish listening to yesterday's broadcast before starting today's. It just transmits, on
schedule, whether the audience is one process or one hundred.

Each consumer here is a radio tuned to `grocery_events`, listening for `order.placed`.
`inventory-consumer`, `notification-consumer`, and `analytics-consumer` were already tuned in
when the station went live. `loyalty-consumer` bought a radio and tuned in later — the station
never had to change its transmitter, its schedule, or its programming to be heard by one more
listener. That's the whole trick: the broadcaster couples to a frequency, not to a list of
listeners.

## Project layout

```
09-event-driven-architecture/
├── docker-compose.yml          order-db, inventory-db, rabbitmq, and all 5 apps
├── .env.example
├── order-api/                  producer — POST /orders, GET /orders/{id}
│   └── src/main/java/com/systemdesign/freshcart/orderapi/
│       ├── orders/              Order, OrderItem entities; OrderPlacedEvent; OrderService
│       │                        (publish-after-commit); OrderController; OrderRepository
│       ├── rabbitmq/            RabbitMqConstants, RabbitMqConfig (exchange only — no queues)
│       └── shared/exception/    NotFoundException + @RestControllerAdvice
├── inventory-consumer/          GET /stock — decrements stock in its own inventory-db
│   └── .../stock/                StockItem entity, StockService, StockConsumer, StockController
├── notification-consumer/       GET /notifications — in-memory
├── analytics-consumer/          GET /stats — in-memory
└── loyalty-consumer/            GET /points — idempotent, in-memory
    └── .../scripts/SimulateDuplicateDeliveryApp.java   duplicate-delivery proof
```

Each app is a fully independent Maven module: its own `pom.xml`, its own `Dockerfile`, its own
`@SpringBootApplication`. None of them depend on each other or share a JAR. The only thing they
share is an agreement on the exchange name, routing key, and JSON event shape — documented above,
not enforced by a shared library. Each app therefore declares its **own** copy of the
`OrderPlacedEvent` record. That's not an oversight: Spring AMQP's `Jackson2JsonMessageConverter`
defaults to `TypePrecedence.INFERRED`, meaning it deserializes into whatever type the
`@RabbitListener` method parameter declares, rather than trusting the producer's `__TypeId__`
message header (which would otherwise name `order-api`'s own, differently-packaged, class and
fail to resolve in a consumer's JVM that never has `order-api` on its classpath). That single
Spring AMQP default is what makes "five independent JARs, zero shared code, still able to parse
each other's JSON" work without any extra configuration.

## Run it

> **Hosting & deployment:** See [HOSTING.md](./HOSTING.md) for Docker setup, platforms (free →
> paid), production tooling, and per-component checklists. **API docs:** Swagger UI at `/docs`
> (springdoc-openapi) on `order-api` only — the four consumers have no HTTP write surface, only
> plain inspection `GET` endpoints.

### Prerequisites

Docker (Postgres ×2, RabbitMQ) + Java 21 / Maven for `mvn package` locally.

### 1. Build and validate each app (no Docker needed for this part)

```bash
for app in order-api inventory-consumer notification-consumer analytics-consumer loyalty-consumer; do
  (cd "$app" && mvn -B clean package)
done
```

Each `mvn package` should complete with `BUILD SUCCESS` and produce a runnable
`target/<app>.jar`.

### 2. Start everything with Docker

```bash
cp .env.example .env
docker compose up --build -d
```

This starts `order-db`, `inventory-db`, `rabbitmq` (management UI at
http://localhost:15672), `order-api` (port `3009`, Swagger at `/docs`), and all four consumers
(`4101`–`4104`).

### Try it

Place an order:

```bash
curl -s -X POST http://localhost:3009/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "customer-42",
    "items": [
      { "sku": "milk-1l", "name": "Whole Milk 1L", "quantity": 2, "unitPrice": 1.5 },
      { "sku": "bread-1", "name": "Sliced White Bread", "quantity": 1, "unitPrice": 2.0 }
    ]
  }' | jq
```

That response comes back almost immediately — `order-api` never waits on any consumer. Now
confirm all four reacted independently:

```bash
curl -s http://localhost:4101/stock | jq          # milk-1l and bread-1 quantities decremented
curl -s http://localhost:4102/notifications | jq  # a push notification for the order
curl -s http://localhost:4103/stats | jq          # ordersToday/revenueToday incremented
curl -s http://localhost:4104/points | jq         # customer-42 awarded points
```

Then run the idempotency demo (see above):

```bash
cd loyalty-consumer
mvn compile exec:java -Dexec.mainClass=com.systemdesign.freshcart.loyaltyconsumer.scripts.SimulateDuplicateDeliveryApp
curl -s http://localhost:4104/points | jq
```

### Stop and reset

```bash
docker compose down -v
```

## Tests

```bash
cd order-api && mvn test
```

`OrderServiceTest` proves `OrderService.placeOrder` saves the order (via the mocked
`TransactionTemplate`/`OrderRepository`) strictly before it publishes `order.placed` (via a mocked
`AmqpTemplate`), and that the published event carries the right `orderId`/`customerId`/
`totalAmount` — the publish-after-commit ordering guarantee, tested without a real database or
broker. No broker-dependent integration tests are included beyond that; the fan-out and
idempotency behavior are demonstrated live against a real RabbitMQ instance instead (the `curl`
walkthrough and `SimulateDuplicateDeliveryApp` above), which is a stronger proof than a mocked
unit test for this specific concept.

## Deviations from the TypeScript original

- **Order items storage** — the TypeScript `order-api` stores `items` as a single `jsonb` column
  on the `orders` table. This port uses a normalized `order_items` table with a foreign key to
  `orders` (Spring Data JPA `@OneToMany`), Hibernate/JPA's idiomatic mapping, and the more
  conventional relational shape for `01-modular-monolith`/`03-async-queue-processing` in this
  Java repo. The wire format every consumer sees (`items` as a JSON array inside the event
  payload) is unchanged.
- **RabbitMQ client** — the TypeScript apps use `amqp-connection-manager`/`amqplib` directly with
  hand-rolled `assertTopology`/`consume` helpers. This port uses Spring AMQP's declarative
  `@Bean`s (`TopicExchange`/`Queue`/`Binding`) plus `@RabbitListener`, which is the idiomatic
  Spring Boot equivalent and needs no custom connection/retry plumbing for this project's needs
  (no DLQ/retry topology here — that's `03-async-queue-processing`'s job).
- **Duplicate-delivery script language** — the TypeScript version is a `ts-node` script
  (`scripts/simulate-duplicate-delivery.ts`) run via `npm run simulate:duplicate`. This port is a
  standalone Java `main()` (`SimulateDuplicateDeliveryApp`) using `com.rabbitmq.client` directly,
  run via `exec-maven-plugin` — the same convention `03-async-queue-processing` already
  established in this repo for its standalone Kafka scripts, so there's exactly one pattern for
  "small non-Spring-context Java program" across the Java ports rather than two.
- **Lombok removed** — the toolchain used to build and validate this specific port runs a JDK
  newer than the Lombok release Spring Boot 3.3.5 manages (and newer than every published Lombok
  release available at build time) supports as an annotation-processing host, which made Lombok
  silently produce no getters/setters/logger fields at all — not a build error, just missing
  symbols downstream, the worst kind of failure to debug. Every entity/service in this project
  uses plain explicit getters/setters and `org.slf4j.LoggerFactory.getLogger(...)` fields instead
  of `@Getter`/`@Setter`/`@Slf4j`, unlike `01-modular-monolith`/`03-async-queue-processing`/
  `05-resilience`, which do use Lombok. If your JDK is JDK 21 (this repo's documented target) or
  another Lombok-compatible version, Lombok would work fine here too — it was avoided purely to
  keep `mvn clean package` passing unconditionally in the environment this project was actually
  built and validated in.

## Validation results (from building this project)

- `mvn -B clean package` — ran and passed (`BUILD SUCCESS`) in all five apps independently.
- `docker compose up --build -d` — all five apps plus `order-db`, `inventory-db`, and `rabbitmq`
  reached a healthy/running state.
- `POST /orders` against `order-api` returned `201` with the persisted order in well under a
  second.
- `GET /stock`, `GET /notifications`, `GET /stats`, and `GET /points` on the four consumers each
  independently reflected the order — confirming true fan-out (one publish, four independent
  reactions), not point-to-point delivery.
- `SimulateDuplicateDeliveryApp` sent the identical `eventId` twice directly to
  `loyalty.order-placed.queue`; `GET /points` showed the demo customer at 42 points (not 84) and
  `processedEventCount` incremented by exactly 1 — the idempotency guard worked against a real
  redelivered duplicate, not a mocked one.
- `docker compose down -v` cleanly tore the stack (and its volumes) back down.
