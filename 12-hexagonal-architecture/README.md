# Orbit — Hexagonal Architecture (Ports & Adapters)

Project 12 of the system design series, Java/Spring Boot edition. This one demonstrates
**Hexagonal Architecture** (a.k.a. Ports & Adapters): the core business logic must not depend on
any framework, database, or external API. Instead the core defines **ports** (interfaces)
describing what it needs, and **adapters** (implementations) plug into those ports from the
outside.

Domain: **Orbit**, a subscription billing service. Plans are `basic`, `pro`, `enterprise`;
customers can subscribe, upgrade, downgrade, and cancel. Java/Spring Boot port of the NestJS
`12-hexagonal-architecture` project in the sibling `system-design` repo.

## Concept, in my own words

In a typical layered app, the "business logic" layer imports its database layer directly —
`SubscriptionService` imports `SubscriptionRepository`, which imports Spring Data JPA and, through
it, Postgres. The business logic *depends on* infrastructure. That's convenient until you want to
unit test a business rule without spinning up Postgres, or swap a payment provider, and suddenly
the business logic and the infrastructure are welded together.

Hexagonal architecture inverts that dependency. The core (`core/`) defines **interfaces** for
everything it needs from the outside world — `SubscriptionRepositoryPort`, `PaymentGatewayPort`,
`NotifierPort` — and never imports a concrete implementation of any of them. Infrastructure code
(Postgres/JPA, Stripe, a REST controller) then depends on the core's interfaces, not the other way
around. This is **dependency inversion**: the arrows that used to point "core → infrastructure"
now point "infrastructure → core."

Two kinds of adapters plug into the hexagon from opposite sides:

- **Inbound / driving adapters** call *into* the core through **input ports** (`SubscribePort`,
  `ChangePlanPort`, `CancelPort`, `GetSubscriptionPort`). In this project: a REST controller and a
  CLI.
- **Outbound / driven adapters** are called *by* the core through **output ports**
  (`SubscriptionRepositoryPort`, `PaymentGatewayPort`, `NotifierPort`) that the core itself
  defines. In this project: a Postgres repository and an in-memory repository (same port, either
  one), a Stripe mock and a Flutterwave mock (same port, either one), and a console notifier.

The payoff, demonstrated for real in this repo rather than just claimed:

1. You can swap Postgres for an in-memory store without touching `core/` — one env var.
2. You can swap payment providers without touching `core/` — one env var.
3. You can unit-test the core with zero DB and zero network by injecting fake adapters — see
   [`src/test/java/.../core/application`](./src/test/java/com/systemdesign/orbit/core/application).

## Diagram

```text
                              INBOUND / DRIVING                    OUTBOUND / DRIVEN
                              (call INTO the core)                 (called BY the core)

                        ┌───────────────────┐                ┌───────────────────────────┐
                        │ SubscriptionController│              │ PostgresSubscription       │
                        │ (adapters/in/http)  │                │ Repository (Spring Data JPA)│
                        └──────────┬──────────┘                │       — or —                │
                                   │                            │ InMemorySubscription        │
                        ┌───────────────────┐                  │ Repository                  │
                        │  OrbitCliRunner     │                 └──────────────▲──────────────┘
                        │ (adapters/in/cli)   │                                │ implements
                        └──────────┬──────────┘                ┌───────────────┴──────────────┐
                                   │ calls                       │ SubscriptionRepositoryPort   │
                                   │                              └───────────────▲──────────────┘
              ┌────────────────────────────────────────────────────────────────┐│
              │                                                                ││
   in ports   │        ╔══════════════════════════════════════════╗           ││   out ports
  (SubscribePort,       ║              CORE (core/)                 ║          │(SubscriptionRepositoryPort,
   ChangePlanPort,      ║                                            ║          │ PaymentGatewayPort,
   CancelPort,          ║   domain/     Subscription.java             ║         │ NotifierPort)
   GetSubscriptionPort) ║               Plan.java                     ║         │
              │         ║               BillingCycle.java             ║         │
              │         ║                                             ║         │
              │         ║   application/ SubscribeUseCase             ║─────────┘ calls out through
              │         ║                ChangePlanUseCase            ║           the port interfaces
              │         ║                CancelUseCase                ║
              │         ║                GetSubscriptionUseCase       ║─────────┐
              │         ║                                             ║         │ calls out through
              │         ║   ZERO Spring / JPA imports anywhere here   ║         │ the port interfaces
              │         ╚═════════════════════════════════════════════╝        │
              └────────────────────────────────────────────────────────────────┘│
                                                                                  ▼
                                                            ┌───────────────────────────┐
                                                            │ PaymentGatewayPort          │
                                                            │ NotifierPort                │
                                                            └──────────────▲──────────────┘
                                                                           │ implements
                                                        ┌──────────────────┴──────────────────┐
                                                        │  StripeMockAdapter                     │
                                                        │      — or —                            │
                                                        │  FlutterwaveMockAdapter                 │
                                                        │                                         │
                                                        │  ConsoleNotifierAdapter                 │
                                                        └─────────────────────────────────────────┘
```

The core sits in the middle knowing nothing about REST, CLI, Postgres, JPA, Stripe, or
Flutterwave — only about the **shapes** of the ports it defined for itself. Everything outside the
double-lined box is replaceable.

## What's in each package

```text
src/main/java/com/systemdesign/orbit/
  core/                              # ZERO org.springframework / jakarta.persistence imports —
                                      # verify: grep -rln -E "org\.springframework|jakarta\.persistence" core/
    domain/
      Subscription.java               # pure class + all 4 business rules
      Plan.java                       # plan catalog (basic/pro/enterprise) + price lookup
      BillingCycle.java               # proration math, pure static methods
      *Error.java                     # plain RuntimeException subclasses, no HTTP knowledge
    ports/
      in/                              # input ports — what use cases the core exposes
      out/                             # output ports — what the core needs from the outside
    application/
      SubscribeUseCase.java           # implements the in ports by orchestrating the out ports
      ChangePlanUseCase.java          #  — this is where the four business rules get exercised
      CancelUseCase.java
      GetSubscriptionUseCase.java
  adapters/
    in/
      http/                            # inbound adapter #1: REST controller + DTOs
      cli/                             # inbound adapter #2: OrbitCliRunner
    out/
      persistence/                     # outbound adapter: Postgres/JPA repo + in-memory repo (same port)
      payment/                         # outbound adapter: Stripe mock + Flutterwave mock (same port)
      notification/                    # outbound adapter: console notifier
  config/
    CoreBeansConfig.java               # reads app.repository / app.payment-provider, binds adapters to ports
    PostgresPersistenceConfig.java     # @Profile("postgres") — the only place a DB connection is attempted
    OpenApiConfig.java
  OrbitApplication.java                # main class; activates the "postgres" profile from APP_REPOSITORY
```

## Business rules (all four live only in `core/`)

1. **Downgrade rejected mid-cycle** — `Subscription.previewPlanChange()` throws
   `DowngradeNotAllowedMidCycleError` if the new plan is cheaper than the current one and the
   current billing period (`currentPeriodEnd`) hasn't ended yet.
2. **Upgrade prorated immediately** — an upgrade mid-cycle is allowed and triggers an immediate
   charge: `(newPrice - oldPrice) * daysRemaining / daysInPeriod`, rounded to 2 decimals. See
   [`BillingCycle.java`](./src/main/java/com/systemdesign/orbit/core/domain/BillingCycle.java)
   `computeProration()`.
3. **Cancel schedules, doesn't delete** — `Subscription.cancel()` sets `cancelAtPeriodEnd = true`;
   nothing is deleted or deactivated immediately, and the subscription stays active until
   `currentPeriodEnd`.
4. **One active subscription per customer** — `SubscribeUseCase` rejects a new subscription with
   `CustomerAlreadySubscribedError` if the customer already has one that's still active.

## Run it

> **Hosting & deployment:** See [HOSTING.md](./HOSTING.md) for Docker setup, platforms, and
> per-component checklists.

```bash
cp .env.example .env
mvn spring-boot:run
```

Swagger UI: `http://localhost:3012/docs` — the OpenAPI description shows which adapters are active
(`REPOSITORY=...`, `PAYMENT_PROVIDER=...`).

### Endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/subscriptions` | Subscribe a customer to a plan (charges the plan price) |
| `POST` | `/subscriptions/{id}/change-plan` | Upgrade (prorated charge) or attempt a downgrade (rejected mid-cycle) |
| `POST` | `/subscriptions/{id}/cancel` | Schedule cancellation at period end |
| `GET` | `/subscriptions/{id}` | Read a subscription |

## Swap a database — the actual walkthrough

Default is `APP_REPOSITORY=memory` (no DB needed). To swap to Postgres:

```bash
# in .env
APP_REPOSITORY=postgres
```

Restart the app (`mvn spring-boot:run`, or `docker compose up` if using Docker — see HOSTING.md).
That's it. **The file that changes is `.env`. The number of files inside `core/` that change is
zero** — verify it yourself:

```bash
grep -rl "APP_REPOSITORY\|app.repository" src/main/java/com/systemdesign/orbit/core   # no results
```

The only code involved is
[`config/CoreBeansConfig.java`](./src/main/java/com/systemdesign/orbit/config/CoreBeansConfig.java),
which reads `app.repository` and binds `SubscriptionRepositoryPort` to either
`PostgresSubscriptionRepository` (Spring Data JPA,
`adapters/out/persistence/PostgresSubscriptionRepository.java`) or
`InMemorySubscriptionRepository` (a `ConcurrentHashMap`,
`adapters/out/persistence/InMemorySubscriptionRepository.java`) — both implement the exact same
`SubscriptionRepositoryPort` interface the core defined. (Wiring the actual JPA/DataSource/Flyway
infrastructure only when Postgres is selected is
[`config/PostgresPersistenceConfig.java`](./src/main/java/com/systemdesign/orbit/config/PostgresPersistenceConfig.java)'s
job — `OrbitApplication` excludes that infrastructure by default so `APP_REPOSITORY=memory` never
attempts a database connection at all.)

## Swap a payment provider — the actual walkthrough

Default is `APP_PAYMENT_PROVIDER=stripe`. To swap:

```bash
# in .env
APP_PAYMENT_PROVIDER=flutterwave
```

Restart the app. Again, zero files inside `core/` change — `CoreBeansConfig` binds
`PaymentGatewayPort` to either `StripeMockAdapter` or `FlutterwaveMockAdapter`
(`adapters/out/payment/`), both implementing `PaymentGatewayPort`. Both are simulated (small
random latency, small *deterministic* failure rate so demos are reproducible — Stripe declines
every 11th charge, Flutterwave every 13th) — neither calls a real external API.

## Proof: CLI and HTTP both drive the same core

The REST controller and the CLI runner are both `@Autowired` with the exact same use-case
singleton beans (`SubscribeUseCase`, `ChangePlanUseCase`, `CancelUseCase`,
`GetSubscriptionUseCase`) that `CoreBeansConfig` constructs — see
[`SubscriptionController.java`](./src/main/java/com/systemdesign/orbit/adapters/in/http/SubscriptionController.java)
and
[`OrbitCliRunner.java`](./src/main/java/com/systemdesign/orbit/adapters/in/cli/OrbitCliRunner.java).
Point them at the same database and you get identical behavior from two different front doors.
This was verified end to end: a subscription created via curl was read back — same id, same
fields — through the CLI's `get` command, and vice versa.

**Via curl (HTTP adapter), against `docker compose up` (APP_REPOSITORY=postgres by default there):**

```bash
curl -s -X POST http://localhost:3012/subscriptions \
  -H 'Content-Type: application/json' \
  -d '{"customerId": "cust-http-1", "planId": "basic"}'
# => {"id":"<uuid>", "customerId":"cust-http-1", "planId":"basic", ...}
```

**Via the CLI (same use-case beans, same rules), pointed at the same Postgres database** (e.g. the
one `docker compose` exposes on `localhost:5432` — see HOSTING.md):

```bash
APP_REPOSITORY=postgres POSTGRES_HOST=localhost POSTGRES_PORT=5432 \
POSTGRES_USER=orbit POSTGRES_PASSWORD=orbit_dev_password POSTGRES_DB=orbit \
mvn spring-boot:run -Dspring-boot.run.profiles=cli,postgres \
    -Dspring-boot.run.arguments="get --id=<id-from-curl-response>"
```

Note two things about that command, both load-bearing:

- `-Dspring-boot.run.profiles=cli,postgres` activates **two** Spring profiles: `cli` (runs
  `OrbitCliRunner` instead of starting a web server) and `postgres` (wires the JPA/DataSource/
  Flyway infrastructure — normally activated automatically by `OrbitApplication`'s `main()` when
  `APP_REPOSITORY=postgres`, but that auto-activation only runs for the app's own `main()`
  invocation, so a manual profile list is the explicit equivalent here).
- `APP_REPOSITORY=postgres` still has to be set too — the Spring **profile** wires the JPA
  infrastructure, but `CoreBeansConfig` independently reads the `app.repository` **property**
  (from this exact env var) to decide which adapter to bind. The two are deliberately separate
  mechanisms; both need to point at Postgres for the CLI to actually read/write real rows.
- `spring-boot.run.arguments` is **space-delimited** (per the Spring Boot Maven Plugin's own
  docs), not comma-delimited — always quote the whole value.

Both produce a subscription with the same shape (`id`, `customerId`, `planId`,
`currentPeriodStart`, `currentPeriodEnd`, `cancelAtPeriodEnd`, `createdAt`, `updatedAt`), enforce
the same four business rules, and — when pointed at the same database — read/write the same
`subscriptions` table.

The CLI also runs completely standalone with no database at all (`APP_REPOSITORY=memory`, the
default, just the `cli` profile) for a quick sanity check:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=cli \
    -Dspring-boot.run.arguments="subscribe --customer=cust-standalone-1 --plan=pro"
```

Other CLI commands:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=cli \
    -Dspring-boot.run.arguments="change-plan --id=<id> --plan=enterprise"
mvn spring-boot:run -Dspring-boot.run.profiles=cli \
    -Dspring-boot.run.arguments="cancel --id=<id>"
```

## Why testing is fast here

[`SubscribeUseCaseTest.java`](./src/test/java/com/systemdesign/orbit/core/application/SubscribeUseCaseTest.java),
[`ChangePlanUseCaseTest.java`](./src/test/java/com/systemdesign/orbit/core/application/ChangePlanUseCaseTest.java),
and
[`CancelUseCaseTest.java`](./src/test/java/com/systemdesign/orbit/core/application/CancelUseCaseTest.java)
test all four business rules — subscribe, upgrade proration math, downgrade-rejected-mid-cycle,
and cancel — by constructing use cases directly with `new`:

```java
SubscribeUseCase useCase = new SubscribeUseCase(
    new InMemorySubscriptionRepository(),
    new AlwaysSucceedsPaymentGateway(),
    new RecordingNotifier());
```

using:

- the **real** `InMemorySubscriptionRepository` adapter (no DB, just a `ConcurrentHashMap`)
- trivial fake `PaymentGatewayPort` doubles (`AlwaysSucceedsPaymentGateway` /
  `AlwaysFailsPaymentGateway` in
  [`core/testsupport/`](./src/test/java/com/systemdesign/orbit/core/testsupport))
- a trivial fake `NotifierPort` (`RecordingNotifier`)

**Zero `@SpringBootTest`, zero Postgres, zero HTTP server, zero Spring `ApplicationContext`
anywhere in these files.** Run them:

```bash
mvn test
```

[`BillingCycleTest.java`](./src/test/java/com/systemdesign/orbit/core/domain/BillingCycleTest.java)
goes one level deeper and tests the proration formula itself as plain static methods, no objects
involved at all — including the exact case from this README: a $9→$29 upgrade with 15 of 30 days
remaining charges exactly `(29 - 9) * 15 / 30 = $10.00`.

This is the actual payoff of ports & adapters: because the core only depends on interfaces it
defined, a test can hand it any implementation — including one that's just a `Map` and a
counter — and the business rule under test runs in milliseconds with no setup/teardown of any
infrastructure. 14 tests, all of them finish in well under a second.

## Contrast with Layered Architecture

`11-layered-architecture` in this series organizes code into horizontal layers (controller →
service → repository) where dependencies flow **downward**: the service layer imports the
repository layer, which imports the database driver. That's simple to read top to bottom, but it
means the business logic layer directly depends on infrastructure — testing a service usually
means mocking or spinning up whatever the repository layer talks to, and swapping the database
means touching the service layer's imports.

Hexagonal architecture inverts those arrows. The core never imports an adapter; adapters import
the core's port interfaces. The core is never at the bottom of an import chain pointing at a
database driver — infrastructure always points inward, toward interfaces the core defines for
itself. The trade-off is more indirection (an interface plus at least one implementation for
everything the core touches) in exchange for a core that can be tested and evolved independently
of whatever database or API happens to be plugged in this week.

## Tests

```bash
mvn test
```

14 tests: 6 in `BillingCycleTest` (pure proration math), 3 in `SubscribeUseCaseTest`, 3 in
`ChangePlanUseCaseTest`, 2 in `CancelUseCaseTest` — see
[Why testing is fast here](#why-testing-is-fast-here) above.

## Related projects

| Project | What it teaches |
|---|---|
| `01-modular-monolith` | Module boundaries within a single deployable |
| `11-layered-architecture` | The layered alternative this project contrasts with |
| `05-resilience` | Retries/circuit-breakers around an outbound adapter's failures |
