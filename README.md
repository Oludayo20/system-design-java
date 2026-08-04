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
Spring Data JPA repositories, Spring AMQP / Spring Kafka listeners, Spring Data Redis).

## Running any project

Every project follows the same pattern:

```bash
cd 0N-project-name
cp .env.example .env
docker compose up -d          # starts Postgres/Redis/RabbitMQ/etc.
mvn spring-boot:run           # or: mvn clean package && java -jar target/*.jar
```

See each project's own README for exact endpoints and things to try.

> Note: this port was written without a local Java/Maven toolchain available to verify
> compilation (only a JRE was present, no `mvn` on PATH). Each project was written carefully
> against the Spring Boot 3 / Java 21 APIs, but run `mvn clean verify` after cloning to catch any
> issues before relying on it.
