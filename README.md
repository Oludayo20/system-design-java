# System Design (Java / Spring Boot edition)

This is a Java/Spring Boot port of [`system-design`](../system-design), which itself turns the
concepts covered in `doc.md` into real, runnable projects and then combines the first three into a
capstone. Projects 05 and 06 add **Level 6 (Resilience)** and **Level 7 (CAP Theorem)**. Every
project uses PostgreSQL in Docker and the same tools the doc names for production use (RabbitMQ,
Redis, Kafka, Nginx), built with Spring Boot 3 / Java 21, Maven, and Spring Data JPA (with Flyway
migrations) instead of NestJS/TypeScript and TypeORM.

| # | Project | Concept in doc.md | Stack |
|---|---------|--------------------|-------|
| 1 | [`01-modular-monolith`](./01-modular-monolith) | Modular monolith architecture (Catalog/Basket/Ordering/Identity, single repo, single deploy, event-driven module boundaries) | Spring Boot, PostgreSQL, Redis, RabbitMQ, Docker Compose |
| 2 | [`02-database-sharding`](./02-database-sharding) | Sharding a database by key (range / hash / geo), shard routing, sharding vs replication | Spring Boot, 3x PostgreSQL shards, Docker Compose |
| 3 | [`03-async-queue-processing`](./03-async-queue-processing) | Producer/queue/consumer, RabbitMQ vs Kafka, retries & dead-letter queues, traffic-spike absorption | Spring Boot, PostgreSQL, RabbitMQ, Kafka, Docker Compose |
| 4 | [`04-ecom-marketplace-capstone`](./04-ecom-marketplace-capstone) | Everything above combined into the "Oja/Skoo" architecture from the end of doc.md | Spring Boot (modular monolith), Nginx load balancer, 2 API replicas, sharded Postgres, Redis, RabbitMQ workers, Docker Compose |
| 5 | [`05-resilience`](./05-resilience) | **Level 6:** retries, circuit breakers, provider fallback, graceful degradation | Spring Boot (in-memory flaky payment demo) |
| 6 | [`06-cap-theorem`](./06-cap-theorem) | **Level 7:** CAP tradeoffs — AP product views vs CP wallet debits, partition simulation | Spring Boot (in-memory two-node cluster) |
| 7 | [`07-monolithic-architecture`](./07-monolithic-architecture) | Plain monolith ("BlogStack") — one codebase, one deploy, shared DB, modules calling each other's services directly with no enforced boundaries. Contrast with project 01's enforced event-driven boundaries | Spring Boot, PostgreSQL, Docker Compose |
| 8 | [`08-microservices-architecture`](./08-microservices-architecture) | Microservices ("BookHive") — API gateway + 4 independently deployable/scalable services, each owning its own PostgreSQL database, talking over HTTP, with fault isolation and independent redeploys | Spring MVC gateway, Spring Boot services, 3× PostgreSQL, Docker Compose |
| 9 | [`09-event-driven-architecture`](./09-event-driven-architecture) | Event-driven pub/sub ("FreshCart") — one `OrderPlaced` event fanning out to 4 independent consumers, loose coupling (add a consumer with zero producer changes), publish-after-commit, idempotent duplicate handling | Spring Boot producer + 4 consumers, PostgreSQL, RabbitMQ, Docker Compose |
| 10 | [`10-serverless-architecture`](./10-serverless-architecture) | Serverless / FaaS — hand-built local Lambda-style emulator with real measured cold vs warm starts, TTL-based scale-to-zero, per-invocation billing, and HTTP/schedule/queue/file-drop triggers | Spring Boot custom runtime, RabbitMQ, Docker Compose |
| 11 | [`11-layered-architecture`](./11-layered-architecture) | Layered / N-Tier ("Riverside Library") — Presentation → Application → Domain → Data Access → Database, with a framework-free Domain layer and business rules unit-tested with zero DB | Spring Boot, Spring Data JPA, PostgreSQL, Docker Compose |
| 12 | [`12-hexagonal-architecture`](./12-hexagonal-architecture) | Hexagonal / Ports & Adapters ("Orbit") — framework-free core with swappable outbound adapters (Postgres ↔ in-memory repo, Stripe ↔ Flutterwave mock payment) and two inbound adapters (REST + CLI) driving the same use cases | Spring Boot, Spring Data JPA, PostgreSQL, Docker Compose |

Each project has a **[HOSTING.md](./01-modular-monolith/HOSTING.md)** guide: local Docker setup, platforms (free → paid), production tooling, and per-component checklists.

| Project | Hosting guide |
|---------|---------------|
| 01 Modular monolith | [`01-modular-monolith/HOSTING.md`](./01-modular-monolith/HOSTING.md) |
| 02 Database sharding | [`02-database-sharding/HOSTING.md`](./02-database-sharding/HOSTING.md) |
| 03 Async queues | [`03-async-queue-processing/HOSTING.md`](./03-async-queue-processing/HOSTING.md) |
| 04 Oja capstone | [`04-ecom-marketplace-capstone/HOSTING.md`](./04-ecom-marketplace-capstone/HOSTING.md) |
| 05 Resilience | [`05-resilience/HOSTING.md`](./05-resilience/HOSTING.md) |
| 06 CAP theorem | [`06-cap-theorem/HOSTING.md`](./06-cap-theorem/HOSTING.md) |
| 07 Monolithic architecture | [`07-monolithic-architecture/HOSTING.md`](./07-monolithic-architecture/HOSTING.md) |
| 08 Microservices architecture | [`08-microservices-architecture/HOSTING.md`](./08-microservices-architecture/HOSTING.md) |
| 09 Event-driven architecture | [`09-event-driven-architecture/HOSTING.md`](./09-event-driven-architecture/HOSTING.md) |
| 10 Serverless architecture | [`10-serverless-architecture/HOSTING.md`](./10-serverless-architecture/HOSTING.md) |
| 11 Layered architecture | [`11-layered-architecture/HOSTING.md`](./11-layered-architecture/HOSTING.md) |
| 12 Hexagonal architecture | [`12-hexagonal-architecture/HOSTING.md`](./12-hexagonal-architecture/HOSTING.md) |

See [`LEVELS-6-7.md`](./LEVELS-6-7.md) for the full concept write-up on resilience and CAP theorem.

`legacy-inmemory-demo/` is a Java port of the original single-process, in-memory simulation. Like
its TypeScript counterpart, it's kept for reference but superseded by the projects above.

## Why this structure

Each numbered folder is a **standalone, independently runnable project** — its own `pom.xml`,
`docker-compose.yml`, and README explaining the concept and how to run/verify it locally. Nothing
is shared between them on purpose: the goal is for each concept to be learnable and runnable in
isolation before you look at how the capstone wires them together.

This is a direct, module-for-module and endpoint-for-endpoint port of
[`../system-design`](../system-design): same entities, same event flow, same sharding
strategies, same queues and workers — reimplemented with Spring idioms (Spring MVC controllers,
Spring Data JPA repositories, Spring AMQP / Spring Kafka listeners, Spring Data Redis). Projects
07–12 continue that same port, domain-for-domain (BlogStack, BookHive, FreshCart, the FaaS
emulator, Riverside Library, Orbit), covering monolithic, microservices, event-driven,
serverless, layered, and hexagonal architecture.

## Running any project

Every project follows the same pattern:

```bash
cd 0N-project-name
cp .env.example .env
docker compose up -d          # starts Postgres/Redis/RabbitMQ/etc.
mvn spring-boot:run           # or: mvn clean package && java -jar target/*.jar
```

Interactive **Swagger API docs** (springdoc-openapi) are available at `http://localhost:<port>/docs` once the API is running (ports: 3000 for 01–04, 3005 for 05, 3006 for 06, 3007 for 07, 3008 for the 08 gateway, 3009 for 09's order-api, 3010 for 10, 3011 for 11, 3012 for 12). Every HTTP endpoint is documented with request/response schemas.

See each project's own README for exact endpoints and things to try.

> Note: projects 01–06 were written without a local Java/Maven toolchain available to verify
> compilation (only a JRE was present, no `mvn` on PATH) — run `mvn clean verify` after cloning to
> catch any issues before relying on them. Projects 07–12 were built later, with a full Java 21 +
> Maven + Docker toolchain available, and each one was validated end-to-end before being
> considered done: `mvn clean package` (build + tests), then `docker compose up --build` plus a
> live curl walkthrough of every endpoint. See each project's README for the exact validation
> steps that were run and their results.

```bash
# 07 — BlogStack: plain monolith (Postgres + API, port 3007)
cd system-design-java/07-monolithic-architecture && cp .env.example .env && docker compose up --build

# 08 — BookHive: microservices (gateway :3008 + 4 services + 3 Postgres)
cd system-design-java/08-microservices-architecture && cp .env.example .env && docker compose up --build -d

# 09 — FreshCart: event-driven pub/sub (order-api :3009 + 4 consumers + 2 Postgres + RabbitMQ)
cd system-design-java/09-event-driven-architecture && cp .env.example .env && docker compose up --build -d

# 10 — Serverless FaaS emulator (API :3010 + RabbitMQ)
cd system-design-java/10-serverless-architecture && cp .env.example .env && docker compose up --build -d

# 11 — Riverside Library: layered/N-Tier (Postgres + API, port 3011)
cd system-design-java/11-layered-architecture && cp .env.example .env && docker compose up --build

# 12 — Orbit: hexagonal/ports & adapters (Postgres + API, port 3012)
cd system-design-java/12-hexagonal-architecture && cp .env.example .env && docker compose up --build
```
