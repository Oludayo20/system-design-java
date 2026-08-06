# E-Shop — Modular Monolith Reference Implementation (Java / Spring Boot)

Project 1 of a 4-part system design series, ported from the original NestJS/TypeScript
implementation to Java/Spring Boot. This one demonstrates the **modular monolith** pattern: one
codebase, one deployment, one repository — but internally organized into modules with strict
boundaries, communicating through **domain events** instead of direct calls.

Think Shopify: a single Rails/Java/whatever app, not a swarm of microservices, but you'd never
know it from how cleanly the code is separated.

## Concept, in my own words

A "monolith" gets a bad reputation because most monoliths are **big balls of mud**: a
`controllers/`, `helpers/`, `services/`, `utils/` soup where every file can call every other file,
and changing checkout code accidentally breaks the search page. That's not what "monolith" has to
mean — it's just what happens when nobody enforces boundaries.

A **modular monolith** takes the same single deployable and slices it into modules that each own
one business area end-to-end (controller, service, repository, entities, events) and are not
allowed to reach into each other's internals. The only sanctioned ways to cross a module boundary
are:

1. A **narrow, exported service method** (e.g. `CatalogService.getProductForOrder(id)`), or
2. A **domain event** published to a shared bus, which other modules may or may not subscribe to.

Nobody injects another module's repository. Nobody runs a SQL join across two modules' tables.
The payoff: you get most of the operational simplicity of a monolith (one process to run, one
transaction manager, one deploy) with most of the structural discipline of microservices — and if
one module (say, Notifications) ever needs to become its own service because it's sending
millions of emails a day, you can cut it out cleanly because its boundary was already a real
boundary, not just a package name.

## Module map

| Module | Package | Owns |
|---|---|---|
| Identity | `identity` | Register, login, JWT issuing/validation |
| Catalog | `catalog` | Products, Categories, Redis cache-aside product reads |
| Basket | `basket` | Add/Remove item, cart totals |
| Ordering | `ordering` | Place Order, Cancel Order, Order History |
| Inventory | `inventory` | `order.created` consumer -> decrements stock |
| Notifications | `notifications` | `order.created` consumer -> simulated receipt email (~5s) |

## Where every doc.md concept lives

| doc.md concept | Implementation |
|---|---|
| "Still one codebase, one deployment, one repository" | Single `pom.xml`, single `Dockerfile`, single Spring Boot app bootstrapped in `EshopModularMonolithApplication` |
| Catalog owns Products/Categories | `catalog/` (`entity/Product.java`, `entity/Category.java`, `CatalogController.java`) |
| Basket owns Add/Remove/Totals | `basket/` (coupons intentionally out of scope, same as the source project) |
| Ordering owns Place/Cancel/History | `ordering/` (`OrderingController`: `POST /orders`, `GET /orders`, `GET /orders/{id}`, `POST /orders/{id}/cancel`) |
| Identity owns Login/Registration/JWT/Roles | `identity/` (`IdentityService`, `entity/User.java` has a `roles` column, `security/JwtAuthenticationFilter.java`) |
| "Modules communicate through events, not direct calls" | `infrastructure/rabbitmq/EventBus.java` — `EventBus.publish()`; Ordering never references Inventory or Notifications at all |
| `OrderCreated` event, "only interested modules react" | `infrastructure/rabbitmq/RabbitMqConstants.java` (`ORDER_CREATED`); only `InventoryConsumer` and `NotificationsConsumer` bind queues to it |
| RabbitMQ "at the bottom", async | `infrastructure/rabbitmq/RabbitMqConfig.java` declares the `domain_events` topic exchange; `InventoryConsumer`/`NotificationsConsumer` are independent `@RabbitListener` subscribers |
| create-order fast, send-email slow, don't await both | `OrderingService#placeOrder` commits the DB transaction, fires `eventBus.publish()` **without blocking on it**, and returns immediately; `NotificationsConsumer` has a literal 5s simulated delay |
| PostgreSQL, "every module stores its data" | One Postgres instance, 4 schemas (see below) |
| "give each module its own schema to strengthen boundaries" | `identity`, `catalog`, `basket`, `ordering` schemas — see `db/migration/V1__create_schemas.sql` and every `@Table(schema = "...")` |
| Redis: sessions, cache | `infrastructure/redis/RedisService.java` used for (a) product cache-aside in `CatalogService`, (b) nothing else built out — same scope as the source project |
| Cache-aside on `/catalog/products/{id}` | `CatalogService#getProduct`: check Redis -> miss -> read Postgres -> populate Redis with a TTL |
| API is the single entrypoint that routes to modules | Spring MVC's dispatcher: `POST /orders` -> `OrderingController`, `GET /catalog/products` -> `CatalogController`, all on one HTTP server |
| Docker: `docker compose up` starts everything | `docker-compose.yml` (postgres, redis, rabbitmq, api) + `Dockerfile` |
| "Ordering module cannot directly manipulate Catalog internals" | Only `CatalogService`'s narrow public methods (`getProduct`, `getProductForOrder`, `decrementStock`) are called from other packages; `ProductRepository`/`CategoryRepository` are only injected inside `catalog/` |

## Full Spring Boot layout

```
src/main/java/com/systemdesign/modularmonolith/
├── identity/            (register, login, JWT, roles)
│   ├── entity/User.java
│   ├── security/        (JwtService, JwtAuthenticationFilter, SecurityConfig, DurationParser)
│   └── dto/
├── catalog/
│   ├── entity/{Product,Category}.java
│   └── dto/ProductForOrder.java  (narrow contract exposed to other modules)
├── basket/
│   └── entity/CartItem.java
├── ordering/
│   ├── entity/{Order,OrderItem,OrderStatus}.java
│   └── dto/{OrderCreatedEvent,OrderCancelledEvent}.java
├── inventory/
│   └── InventoryConsumer.java   (order.created -> decrement stock)
├── notifications/
│   └── NotificationsConsumer.java (order.created -> simulated email, 5s)
├── infrastructure/
│   ├── postgres/          (Flyway migrations live under resources/db/migration)
│   ├── redis/RedisService.java
│   └── rabbitmq/          (RabbitMqConfig topology, EventBus, EventEnvelope, RabbitMqConstants)
├── shared/                (CurrentUser resolver, global exception handling)
└── EshopModularMonolithApplication.java
```

## Companies known to use this pattern

- **Shopify** — its Rails monolith is organized into "components" (their term for modules) with
  enforced boundaries, and it still runs a huge share of Shopify's core commerce platform as one
  deployable.
- **GitHub** — ran (and largely still runs) as a modular Rails monolith rather than microservices.
- **Basecamp / 37signals** — publicly advocate for "majestic monolith" architecture.
- **StackOverflow** — famously scaled a monolith (not modularized the same way, but the same
  "you don't need microservices to scale" lesson applies).

## Running it

> **Hosting & deployment:** See [HOSTING.md](./HOSTING.md) for Docker setup, platforms (free → paid), production tooling, and per-component checklists. **API docs:** Swagger UI at `/docs` (springdoc-openapi).

### Prerequisites

- Docker + Docker Compose (recommended path), **or** Java 21, Maven, PostgreSQL 16, Redis 7,
  RabbitMQ 3 running locally.

### Option A — Docker Compose (everything)

```bash
cp .env.example .env
docker compose up --build
```

This starts Postgres, Redis, RabbitMQ (management UI on http://localhost:15672,
`eshop` / `eshop_dev_password` by default), runs Flyway migrations on boot, then starts the API on
http://localhost:3000 (Swagger UI at http://localhost:3000/docs).

### Option B — Local Maven, infra in Docker

```bash
cp .env.example .env
docker compose up -d postgres redis rabbitmq
mvn spring-boot:run
```

### Useful commands

```bash
mvn clean package    # build the jar (also runs tests)
mvn spring-boot:run  # run locally
mvn test             # unit tests only
```

## Curl walkthrough

The seed migration (`V6__seed_catalog_demo_data.sql`) creates demo products across a few
categories, so you can run this immediately after `docker compose up`.

```bash
BASE=http://localhost:3000

# 1. Register
curl -s -X POST $BASE/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"S3curePassword!","fullName":"Jane Doe"}' | tee /tmp/register.json

TOKEN=$(node -p "require('/tmp/register.json').accessToken" 2>/dev/null || python3 -c "import json;print(json.load(open('/tmp/register.json'))['accessToken'])")

# 2. (or) Login instead of register on subsequent runs
curl -s -X POST $BASE/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"jane@example.com","password":"S3curePassword!"}'

# 3. Browse products (Catalog module; GET /catalog/products/{id} is Redis cache-aside)
curl -s $BASE/catalog/products | tee /tmp/products.json

curl -s $BASE/catalog/products/<productId>   # first call: Postgres, populates Redis
curl -s $BASE/catalog/products/<productId>   # second call: served from Redis (check logs for "Cache hit")

# 4. Add to basket (Basket module; resolves price via CatalogService, not Catalog's table)
curl -s -X POST $BASE/basket/items \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"productId":"<productId>","quantity":1}'

curl -s $BASE/basket -H "Authorization: Bearer $TOKEN"

# 5. Place the order — note how fast the response comes back
curl -s -X POST $BASE/orders -H "Authorization: Bearer $TOKEN"
```

You'll see `{"success":true,"orderId":"..."}` come back well under a second — the order is
already committed to Postgres and `order.created` has already been published. Watch the API logs
(`docker compose logs -f api`) and you'll see, independently and asynchronously:

```
InventoryConsumer   : Reducing stock for order <id> (1 line item(s))
InventoryConsumer   : Decremented stock for product <id> by 1
InventoryConsumer   : Stock reduction complete for order <id>
NotificationsConsumer: Sending receipt email for order <id> to user <id> (simulated, ~5s)...
...about 5 seconds later...
NotificationsConsumer: Receipt email sent for order <id>, total $89.98
```

The HTTP response returned before the notification log line even started — that's the whole
point: create-order is fast, send-email is slow, and the customer never waits on the slow part.

```bash
# 6. Order history / detail
curl -s $BASE/orders -H "Authorization: Bearer $TOKEN"
curl -s $BASE/orders/<orderId> -H "Authorization: Bearer $TOKEN"
```

## Notes on the implementation

- **No `ddl-auto: update`.** Schema is entirely migration-driven (`src/main/resources/db/migration`,
  applied by Flyway on boot), which is what you'd actually ship.
- **Event publish happens after commit, not before** (`OrderingService#placeOrder`): publishing
  before the transaction commits risks a consumer reacting to an order a later rollback erases.
- **Event publish doesn't block the response**: `EventBus#publish` runs the actual RabbitMQ send on
  a virtual-thread executor and returns a `CompletableFuture` that the controller never waits on,
  matching the "API responds almost instantly" behavior in the doc.
- **RabbitMQ topology as code**: the `domain_events` topic exchange and every consumer's durable
  queue + binding are declared as `@Bean`s in `RabbitMqConfig` rather than configured by hand in
  the RabbitMQ UI.
- This is a direct Java/Spring Boot port of the NestJS `01-modular-monolith` project in the sibling
  `system-design/` repo — same modules, same endpoints, same event flow, same Postgres
  schema-per-module layout.
- No Java/Maven/Docker toolchain was available in the environment this port was written in, so the
  compose stack could not be integration-tested end-to-end. Run `mvn clean verify` and
  `docker compose up --build` to validate before relying on it.
