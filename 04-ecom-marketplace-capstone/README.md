# Oja Marketplace — Capstone (Java / Spring Boot port)

A single Spring Boot modular monolith that combines the three concepts from `doc.md` into one
coherent, runnable system: **modular monolith architecture** (project 01), **database sharding**
(project 02), and **async queue processing** (project 03). One codebase, one Docker image, two
horizontally-scaled replicas behind Nginx, one sharded data layer for Users/Wallet, one unsharded
primary for everything else, one RabbitMQ event bus feeding four independent background workers.

This is a faithful Java/Spring Boot port of the original NestJS/TypeScript capstone
(`04-ecom-marketplace-capstone`). Same modules, same REST endpoints, same primary/shard schema
split, same hash-sharding routing, same RabbitMQ event flow, same Redis cache-aside, same JWT
auth, same Nginx load-balancing topology — reimplemented with Spring Boot 3 / Java 21 / Maven
instead of NestJS / TypeScript / npm.

This is **not** microservices. It's one repository, one `mvn package`, one runnable jar, run
twice — exactly the point `doc.md` makes about Shopify-style modular monoliths.

## Architecture

This mirrors doc.md's final combined-system diagram exactly, with Cloudflare called out as
explicitly out of scope for local dev:

```
                                User
                                  │
                                  ▼
                    Cloudflare (CDN) — OUT OF SCOPE for local dev.
                    In production this would sit in front of Nginx for
                    edge caching/DDoS protection; it adds nothing you can
                    observe on localhost, so it's omitted here. Nginx
                    below is the load balancer this repo actually runs.
                                  │
                                  ▼
                       Nginx Load Balancer (:8080)
                        round-robin, /health passthrough
                                  │
                  ┌───────────────┴───────────────┐
                  ▼                                ▼
            API Server 1                     API Server 2
         (same jar, INSTANCE_ID=api-1)   (same jar, INSTANCE_ID=api-2)
                  │                                │
                  │        Both replicas run the same modules:        │
                  │  Auth · Users · Marketplace · Orders · Wallet ·    │
                  │  Health (+ Email/Inventory/Analytics/Wallet        │
                  │  workers, in-process, on both replicas)            │
                  └───────────────┬───────────────┘
                                  │
                ┌─────────────────┼──────────────────┐
                ▼                 ▼                  ▼
      PostgreSQL PRIMARY   Sharded PostgreSQL    Redis (cache & sessions)
      Marketplace/Orders/  (hash(userId) % 3)     product-list cache-aside
      email→shard          ┌─────┬─────┬─────┐
      directory             │  0  │  1  │  2  │  Users + Wallets ONLY
                             └─────┴─────┴─────┘  (see "Why only shard
                                                    Users/Wallet" below)
                                  │
                     Customer places an order (POST /orders)
                                  │
                                  ▼
                    Save Order (PostgreSQL primary, transaction)
                                  │
                                  ▼
                    Publish "order.created" ──────► RabbitMQ
                                  │                  (topic exchange
                                  ▼                   `domain_events`)
                    Return { success: true, orderId }
                    to the client — a few hundred ms,
                    NOT waiting on anything below
                                  │
              ┌───────────┬───────────────┬───────────────┐
              ▼           ▼               ▼               ▼
          Email        Inventory      Analytics         Wallet
          Worker        Worker          Worker         Settlement
       (send receipt) (decrement    (record sale)    Worker (debit the
                        stock via                    correct SHARD via
                        Marketplace)                 ShardRouterService)
```

### Module map (per doc.md's "if you were building Oja or Skoo today")

```
API
 ├── Auth module           — register/login, JWT, BCrypt
 ├── Users module           — sharded profile CRUD
 ├── Marketplace module     — products/categories, Redis cache-aside   (Catalog, renamed)
 ├── Orders module          — validate → persist → publish order.created
 ├── Wallet module          — balance + ledger, colocated with Users' shard
 ├── Workers module         — Email / Inventory / Analytics RabbitMQ consumers
 └── Health module          — polled by Nginx/compose healthchecks
```

`School`, `Payment`, `Chat`, and `Admin` are named in doc.md's proposal but intentionally **not
built** here — same rationale as the original: they'd follow the exact same pattern (a module
with a controller/service/entities, talking to others only through public methods or published
events) and would demonstrate nothing new for this capstone.

## Infra → doc.md concept → sibling project

| Infra piece | doc.md concept it demonstrates | Sibling project that shows it in isolation |
|---|---|---|
| Nginx (`nginx.conf`, port 8080) | "Nginx Load Balancer" in the final combined diagram; round-robin across stateless replicas | *(new to this capstone — no sibling project stands this up)* |
| `api-1` / `api-2` (same jar) | "API Server 1 / API Server 2" — one codebase, two replicas, no shared in-process state | `01-modular-monolith` — module boundaries (Catalog/Basket/Ordering/Identity ↔ Auth/Users/Marketplace/Orders/Wallet here) |
| `postgres-primary` | "Every module stores its data" — Marketplace/Orders on one well-indexed Postgres, per doc.md's explicit sharding guidance | `01-modular-monolith` — schema-per-module Postgres |
| `postgres-shard-0/1/2` | "Real Example: Your Oja Marketplace" — shard Users (and Wallet) by `hash(userId) % 3` | `02-database-sharding` — hash-sharding strategy, shard router |
| `redis` | "Redis (Cache & Sessions)" — cache-aside on `GET /marketplace/products` | `01-modular-monolith` — Redis cache-aside |
| `rabbitmq` + 4 workers | "RabbitMQ at the Bottom" / "Publish order.created → Email/Inventory/Analytics/Wallet Workers" | `03-async-queue-processing` — topic exchange, retry/DLQ via dead-letter-exchange |

## Why only Users/Wallet are sharded (deliberate restraint)

Unchanged from the original: doc.md lays out a 5-stage scaling ladder and is explicit that most
SaaS applications never reach Stage 5 (sharding). This capstone imagines Oja at the point where
Users have genuinely outgrown a single database. Marketplace and Orders have **not** outgrown
anything — a well-indexed products table and a foreign-key-free `user_id` column on orders is
Stage 1-2 territory. So: **Users and Wallet are sharded by `hash(userId) % 3`. Marketplace,
Orders, and health/worker bookkeeping stay on one primary Postgres.**

### Why Wallet is colocated with Users (not sharded independently)

A wallet has no meaning without its owning user, and the two are almost always read/written
together. Both `User` and `Wallet` are keyed by the same shard function (`hash(userId) % 3`), so
they always land on the same physical Postgres instance — every wallet operation is a normal
single-database ACID transaction, never a distributed one.

### The email → shard lookup

Login only has an email, not a userId — but the shard key is `hash(userId) % 3`, not
`hash(email)`. `UserDirectory` (a small, unsharded table on the primary database) answers "which
shard is this email's user on?" `AuthService.register` writes the shard row (User + Wallet) first,
then the directory row, and **compensates** (deletes the shard rows) if the second write fails.
The one gap this doesn't close is a process crash *between* the two writes — production would
replace the synchronous compensating delete with a transactional outbox + reconciliation job. See
`UserDirectory.java` and `AuthService.register` for the full reasoning.

## How the multi-datasource JPA setup works

This is the trickiest part of the port, since Spring Boot's JPA autoconfiguration assumes exactly
one `DataSource`. Auto-configuration for `DataSource`/Hibernate/Spring-Data-JPA-repositories/Flyway
is disabled (`EcomMarketplaceCapstoneApplication`'s `@SpringBootApplication(exclude = {...})` and
`application.yml`'s `spring.autoconfigure.exclude`), and everything is wired by hand instead:

- **`PrimaryDataSourceConfig`** — one `DataSource` (Hikari), one `Flyway` instance pointed at
  `classpath:db/migration/primary`, one `LocalContainerEntityManagerFactoryBean` scanning
  `marketplace.entity` / `orders.entity` / `auth.entity`, one `JpaTransactionManager`. Also
  declares `@EnableJpaRepositories` for `marketplace.repository` / `orders.repository` /
  `auth.repository` — ordinary Spring Data repositories, since there's exactly one primary
  database.
- **`Shard0DataSourceConfig` / `Shard1DataSourceConfig` / `Shard2DataSourceConfig`** — identical
  in shape, one per shard: `DataSource`, a `Flyway` instance pointed at
  `classpath:db/migration/shard` (the **same** SQL file, run independently against three
  databases — each shard keeps its own `flyway_schema_history`), an `EntityManagerFactory`
  scanning `users.entity` / `wallet.entity`, a `JpaTransactionManager`, and a shared,
  transaction-aware `EntityManager` (`SharedEntityManagerCreator`). These are **not** wired via
  `@EnableJpaRepositories`, because the same `UserRepository`/`WalletRepository` interface must
  work against whichever of the 3 databases a given `userId` hashes to — one fixed
  interface-to-`EntityManagerFactory` binding doesn't fit that.
- **`ShardRouterService`** (`sharding/`) — the single choke point for "which physical database
  owns this user?" It holds the 3 shards' `DataSource`/`EntityManager`/`PlatformTransactionManager`
  triples and:
  - `resolveShardIndex(userId)` — delegates to `HashShardingStrategy` (`hash(userId) % 3`, ported
    bit-for-bit from the original's djb2 hash, including the `>>> 0` unsigned-coercion behavior,
    via `Integer.toUnsignedLong`).
  - `getRepository(RepositoryInterface.class, shardIndexOrUserId)` — builds a **live Spring Data
    repository proxy on demand**, via `JpaRepositoryFactory(entityManager).getRepository(...)`,
    bound to the resolved shard's shared `EntityManager`. This is the direct equivalent of the
    original's `dataSource.getRepository(Entity)` — a repository interface that works against
    whichever shard the router resolves to, rather than being permanently bound to one database.
  - `getTransactionTemplate(shardIndex)` — a `TransactionTemplate` scoped to that shard's
    `PlatformTransactionManager`, the equivalent of the original's
    `shardDataSource.transaction(async manager => {...})`.

  Every service that touches `User` or `Wallet` data (`AuthService`, `UsersService`,
  `WalletService`) goes through `ShardRouterService` instead of injecting a shard `DataSource`
  directly — nothing in the app ever scatter-gathers across all three shards.
- Schema ownership: Hibernate is configured with `hibernate.hbm2ddl.auto=validate` everywhere —
  Flyway (ported 1:1 from the original's TypeORM migrations under
  `src/main/resources/db/migration/{primary,shard}`) owns the schema entirely; Hibernate only
  validates the entity mappings against what Flyway already created, matching the original's
  `synchronize: false`.

## Running it

```bash
cd 04-ecom-marketplace-capstone
cp .env.example .env

# Start every piece of infra: Nginx, 2 API replicas, primary + 3 shard Postgres,
# Redis, RabbitMQ - all with healthchecks and service_healthy dependencies.
docker compose up -d --build

# Or, for local (non-Docker) development against dockerized infra:
mvn clean package
java -jar target/ecom-marketplace-capstone.jar
# Flyway runs automatically on startup (once per DataSource: primary, then
# shard0/1/2 - each maintains its own flyway_schema_history table).
```

No separate migration commands are needed: each of the 4 `Flyway` beans
(`primaryFlyway`/`shard0Flyway`/`shard1Flyway`/`shard2Flyway`) runs its `migrate()` automatically
during Spring context startup, before the corresponding `EntityManagerFactory` is created.

## End-to-end walkthrough

Once running (via `docker compose up -d`, hitting Nginx on `:8080`; substitute `:3000` if running
the jar directly against one instance):

```bash
BASE=http://localhost:8080

# 1. Register - generates a userId, hashes it to a shard, writes User+Wallet
#    on that shard, writes the email->shard directory entry on primary, then
#    issues a JWT. Wallet gets a 5,000 (500000 cents) signup bonus so the
#    settlement debit below has something to draw from.
curl -s -X POST $BASE/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@oja.dev","password":"correct horse battery staple","fullName":"Ada Lovelace"}' | tee /tmp/register.json

TOKEN=$(jq -r .accessToken /tmp/register.json)

# 2. Login - resolves the shard via the email->shard directory on primary,
#    then queries exactly one shard database for the user row.
curl -s -X POST $BASE/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"ada@oja.dev","password":"correct horse battery staple"}'

# 3. Browse the marketplace - first call is a Postgres read + Redis populate,
#    every call after that (within 60s) is served straight from Redis.
curl -s $BASE/marketplace/products | jq '.[0]'
PRODUCT_ID=$(curl -s $BASE/marketplace/products | jq -r '.[0].id')

# 4. Check the wallet balance before placing an order.
curl -s $BASE/wallet/me -H "Authorization: Bearer $TOKEN" | jq '.wallet.balanceCents'

# 5. Place an order - validates stock via MarketplaceService, persists
#    Order+OrderItems in a transaction on the PRIMARY db, publishes
#    order.created, and returns immediately. No email/inventory/analytics/
#    wallet work has happened yet at the moment this response arrives.
curl -s -X POST $BASE/orders \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d "{\"items\":[{\"productId\":\"$PRODUCT_ID\",\"quantity\":2}]}"
# => { "success": true, "orderId": "..." }

# 6. Give the workers a moment, then check the wallet again - the balance
#    should have dropped by the order total, debited on the correct SHARD.
sleep 1
curl -s $BASE/wallet/me -H "Authorization: Bearer $TOKEN" | jq '.wallet, .ledger[0]'

# 7. Health check - what Nginx/orchestration polls; verifies primary, all
#    3 shards, Redis and RabbitMQ are all reachable from this replica.
curl -s $BASE/health | jq
```

### Observing the distributed pieces

```bash
# Watch both replicas at once. You'll see requests land on whichever replica
# Nginx's round-robin picked (INSTANCE_ID in the log line tells you which),
# and - a beat later, from a SEPARATE line, on whichever replica happened to
# be the one whose consumer picked the message off the queue - the four
# workers reacting to order.created:
docker compose logs -f api-1 api-2

# Expect to see, spread across the two containers:
#   OrdersService  : Order <id> persisted for user <id> - publishing order.created
#   EmailWorker    : [email-worker] Sending receipt for order <id> to user <id> ...
#   InventoryWorker: [inventory-worker] Order <id>: decrementing stock for product <id> by 2
#   AnalyticsWorker: [analytics-worker] Recorded sale: order <id>, user <id>, 1 line item(s) ...
#   OrderSettlementListener: [wallet-worker] Settling order <id> for user <id>
#   OrderSettlementListener: [wallet-worker] Debited <n> cents from user <id>'s wallet

# RabbitMQ management UI (queues, message rates, the domain_events exchange):
open http://localhost:15672   # user/pass from .env, default oja/oja_dev_password

# Swagger / OpenAPI docs (mirrors the original's SwaggerModule.setup('docs', ...)):
open http://localhost:8080/docs
```

## Endpoints

| Method | Path | Auth | Notes |
|---|---|---|---|
| POST | `/auth/register` | public | Creates User+Wallet on a shard, directory row on primary, returns a JWT |
| POST | `/auth/login` | public | Resolves shard via directory, verifies password, returns a JWT |
| GET | `/users/me` | bearer JWT | Sharded profile read |
| PATCH | `/users/me` | bearer JWT | Sharded profile update |
| GET | `/marketplace/products` | public | Redis cache-aside (60s TTL) |
| GET | `/marketplace/products/{id}` | public | |
| GET | `/marketplace/categories` | public | |
| POST | `/orders` | bearer JWT | Validates stock, persists on primary, publishes `order.created` |
| GET | `/wallet/me` | bearer JWT | Wallet balance + last 20 ledger entries, from the owning shard |
| GET | `/health` | public | Primary + all 3 shards + Redis + RabbitMQ reachability |

## Tests

```bash
mvn test
```

Two test classes, no live infra required:

- `src/test/java/.../sharding/HashShardingStrategyTest.java` and
  `ShardRouterServiceTest.java` — prove `hash(userId) % 3` is deterministic (same id → same
  shard, every call), reproduce doc.md's own worked example (`15 % 3 = 0`, `230 % 3 = 2`,
  `987 % 3 = 0`, `1500 % 3 = 0`) for numeric ids, and assert User/Wallet colocation (same key →
  same shard, by construction) — ported from `shard-router.service.spec.ts` (whose actual
  scenarios exercise `HashShardingStrategy` directly).
- `src/test/java/.../orders/OrdersServiceTest.java` — proves the order flow persists to the
  (mocked) primary DB *before* publishing `order.created` to the (mocked) event bus, that it
  never calls Marketplace's stock-mutating method directly (only the Inventory worker does,
  reacting to the event), and that insufficient stock is rejected before either persistence or
  publish happens — ported from `orders.service.spec.ts`.

## Deviations from the original NestJS project (and why)

- **Explicit UUIDs everywhere, not just for User.** The original relies on Postgres's
  `gen_random_uuid()` default for most primary-keys and only explicitly generates the id for
  `User` (since it's the shard key and must exist before the shard is resolved). This port has
  every entity's id assigned explicitly in application code (`UUID.randomUUID()`) before
  `save()`, rather than round-tripping through a DB-side default. This avoids Hibernate
  `INSERT ... RETURNING` refresh subtleties and makes services (notably `OrdersService`) testable
  with a plain mocked repository, mirroring how `User` already worked in the original. The `uuid
  ... DEFAULT gen_random_uuid()` column defaults remain in the migrations as a harmless safety net
  that the application never actually relies on.
- **JWT secret is stretched to 256 bits via SHA-256.** `io.jsonwebtoken` (jjwt) enforces a minimum
  256-bit HMAC-SHA key; Node's `jsonwebtoken`/`@nestjs/jwt` do not. The sample
  `JWT_SECRET=change_me_in_production_please` is 30 bytes (240 bits), so `JwtService` hashes the
  configured secret with SHA-256 before using it as the signing key, rather than using the raw
  bytes directly — this keeps the sample `.env` working out of the box while staying
  spec-compliant.
- **Shard repositories are built on demand, not `@EnableJpaRepositories`-bound.** See "How the
  multi-datasource JPA setup works" above — this is a necessary adaptation, not a style choice:
  Spring Data JPA has no built-in notion of "the same repository interface, against whichever of N
  databases a runtime value resolves to."
- **Wallet debit/credit locking uses a `@Lock(PESSIMISTIC_WRITE)` repository query** rather than
  TypeORM's `manager.findOne(..., { lock: { mode: 'pessimistic_write' } })` — same
  `SELECT ... FOR UPDATE` behavior, expressed the idiomatic Spring Data way.
- **Error response shape** is a small custom `@RestControllerAdvice`
  (`GlobalExceptionHandler`) reproducing Nest's default exception filter shape
  (`{ statusCode, message, error, timestamp }`) for the four exception types the app actually
  throws (`ConflictException`/`NotFoundException`/`BadRequestException`/`UnauthorizedException`),
  rather than Spring Boot's default `ProblemDetail` shape.

## What wasn't fully verified

No Java, Maven, or Docker toolchain was available in the environment this was built in, so this
was **not** compiled or run — `mvn clean package`, `mvn test`, `docker compose up`, and the full
register → login → order → worker-settlement flow against real infra have not been executed here.
Every file was written and then re-read for `jakarta.*` (not `javax.*`) correctness, import
correctness, and Spring Data/Spring AMQP/Spring Security API shapes, with particular attention to
the multi-datasource Flyway/JPA wiring (the highest-risk part of this port). Everything above is
written to be run as-is once a JDK 21 + Maven + Docker environment is available.

## Levels 6 & 7 — What comes after this capstone

Projects **01–04** get you to a production-shaped architecture: modular monolith, sharding,
async workers, and horizontal scaling. The last two levels are about **behavior when things fail**:

| Level | Concept | Where to learn it | Already in this capstone |
|---|---|---|---|
| 6 | **Resilience** — retries, circuit breakers, fallbacks, graceful degradation | [`05-resilience`](../05-resilience) | Nginx + 2 API replicas; RabbitMQ retry/DLQ from project 03 |
| 7 | **CAP theorem** — AP vs CP tradeoffs during network partitions | [`06-cap-theorem`](../06-cap-theorem) | Wallets on shards (CP); product cache reads (more AP-friendly) |

Full write-up: [`LEVELS-6-7.md`](../LEVELS-6-7.md)

**Oja-specific CAP choices in this capstone:**

- **Wallet balance (CP):** debits go through `ShardRouterService` to the correct shard; insufficient funds rejected before any event is published.
- **Product list cache (AP-friendly):** Redis cache-aside on `GET /marketplace/products` — slightly stale catalog data is acceptable.
- **Order workers (eventual):** inventory/email/analytics settle asynchronously after the HTTP response returns.

**Resilience gaps to add in production (see project 05):**

- Circuit breakers around external payment providers (Paystack/Flutterwave)
- Application-level retry with backoff before falling back to a second provider
- Health-check-driven load balancer draining of unhealthy replicas
