# Hosting & Deployment Guide — Event-Driven Architecture (Java / Spring Boot)

This document covers local Docker setup and production hosting for the FreshCart fan-out demo:
one Spring Boot **producer** (`order-api`), four independent Spring Boot **consumer** apps, two
PostgreSQL databases (each owned by exactly one app), and RabbitMQ (topic exchange, no
retry/DLQ — that topology lives in `03-async-queue-processing`).

For architecture, the fan-out diagram, and the idempotency proof, see [README.md](./README.md).

---

## Application components

| Component | Role | Required? | Local (Docker) service |
|-----------|------|-----------|------------------------|
| **order-api** | `POST /orders` — saves order, publishes `order.placed`, returns immediately | Yes | `order-api` |
| **inventory-consumer** | Decrements stock in its own `inventory-db` | Yes | `inventory-consumer` |
| **notification-consumer** | Logs an in-memory push notification | Yes | `notification-consumer` |
| **analytics-consumer** | Increments in-memory sales counters | Yes | `analytics-consumer` |
| **loyalty-consumer** | Awards loyalty points, idempotently (in-memory) | Yes | `loyalty-consumer` |
| **order-db (PostgreSQL 16)** | Owned exclusively by `order-api` | Yes | `order-db` |
| **inventory-db (PostgreSQL 16)** | Owned exclusively by `inventory-consumer` | Yes | `inventory-db` |
| **RabbitMQ 3** | Topic exchange `grocery_events`; one durable queue per consumer | Yes | `rabbitmq` |

All five apps are **separate Maven modules, separate JARs, separate containers** — unlike
`03-async-queue-processing`'s API/worker split (two entrypoints, one JAR), there is no shared
build artifact here at all. Each app's `Dockerfile` builds and runs independently.

**Stack:** Spring Boot 3.3.5 / Java 21 / Maven / Spring Data JPA + Flyway (`order-api`,
`inventory-consumer` only) / Spring AMQP (`@RabbitListener` + declarative topology beans) /
springdoc-openapi at `/docs` (`order-api` only).

---

## Prerequisites (your machine)

| Tool | Version | Purpose |
|------|---------|---------|
| Docker Desktop | Latest | Postgres ×2, RabbitMQ, all 5 apps |
| Docker Compose | v2+ | Orchestration |
| Java (Temurin) | 21 | Local dev, `mvn package`, the duplicate-delivery script |
| Maven | 3.9+ | Build, test, run |
| curl / jq | Any | API testing |

Optional:

| Tool | Purpose |
|------|---------|
| RabbitMQ Management UI | http://localhost:15672 — queue depth, exchange bindings |
| [pgAdmin](https://www.pgadmin.org/) / `psql` | Inspect `order-db`/`inventory-db` directly |

---

## Run locally with Docker

### 1. Configure

```bash
cd system-design-java/09-event-driven-architecture
cp .env.example .env
```

Key variables:

| Variable | Default | Purpose |
|----------|---------|---------|
| `ORDER_DB_PORT` | `5434` | Host port for `order-db` (avoids clashing with a local Postgres on 5432) |
| `INVENTORY_DB_PORT` | `5433` | Host port for `inventory-db` |
| `RABBITMQ_URL` | `amqp://freshcart:freshcart_password@localhost:5672` | Broker connection (all 5 apps) |
| `INVENTORY_CONSUMER_PORT` / `NOTIFICATION_CONSUMER_PORT` / `ANALYTICS_CONSUMER_PORT` / `LOYALTY_CONSUMER_PORT` | `4101`–`4104` | Each consumer's own inspection-endpoint port |

### 2. Start the full stack

```bash
docker compose up --build -d
```

Starts: `order-db`, `inventory-db`, `rabbitmq`, `order-api` (port `3009`), and all four consumers
(`4101`–`4104`). Every app's `depends_on` waits on its database (if any) and RabbitMQ passing
their `service_healthy` healthcheck before starting, so Flyway migrations and queue declarations
run against infrastructure that's actually ready.

### 3. Verify the producer + fan-out

```bash
curl -s -X POST http://localhost:3009/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerId": "customer-42",
    "items": [{ "sku": "milk-1l", "name": "Whole Milk 1L", "quantity": 2, "unitPrice": 1.5 }]
  }' | jq

curl -s http://localhost:4101/stock | jq
curl -s http://localhost:4102/notifications | jq
curl -s http://localhost:4103/stats | jq
curl -s http://localhost:4104/points | jq
```

Expect a sub-second `201` from `order-api`, and all four `GET`s reflecting the order — each
consumer reacted independently off its own queue.

### 4. Run the idempotency demo

```bash
cd loyalty-consumer
mvn compile exec:java -Dexec.mainClass=com.systemdesign.freshcart.loyaltyconsumer.scripts.SimulateDuplicateDeliveryApp
curl -s http://localhost:4104/points | jq
```

`demo-customer-idempotency` should show `42` points and `processedEventCount` should increase by
exactly `1`, even though the message was delivered twice.

### Option B — Infra in Docker, apps on host

```bash
cp .env.example .env
set -a && source .env && set +a

docker compose up -d order-db inventory-db rabbitmq

# Each in its own terminal, from its own directory:
(cd order-api && mvn spring-boot:run)
(cd inventory-consumer && mvn spring-boot:run)
(cd notification-consumer && mvn spring-boot:run)
(cd analytics-consumer && mvn spring-boot:run)
(cd loyalty-consumer && mvn spring-boot:run)
```

Flyway runs automatically on `order-api`/`inventory-consumer` startup
(`spring.flyway.enabled=true`); `notification-consumer`/`analytics-consumer`/`loyalty-consumer`
have no database at all (in-memory state only).

### Build and run JARs directly

```bash
for app in order-api inventory-consumer notification-consumer analytics-consumer loyalty-consumer; do
  (cd "$app" && mvn -B clean package)
done

set -a && source .env && set +a
java -jar order-api/target/order-api.jar                     &
java -jar inventory-consumer/target/inventory-consumer.jar    &
java -jar notification-consumer/target/notification-consumer.jar &
java -jar analytics-consumer/target/analytics-consumer.jar    &
java -jar loyalty-consumer/target/loyalty-consumer.jar        &
```

### Service reference

| Service | URL / Port | Credentials |
|---------|------------|-------------|
| order-api + Swagger | http://localhost:3009/docs | — |
| order-db (PostgreSQL) | localhost:5434 | `freshcart` / `freshcart_password` |
| inventory-db (PostgreSQL) | localhost:5433 | `freshcart` / `freshcart_password` |
| RabbitMQ AMQP | localhost:5672 | `freshcart` / `freshcart_password` |
| RabbitMQ UI | http://localhost:15672 | same |
| inventory-consumer | http://localhost:4101/stock | — |
| notification-consumer | http://localhost:4102/notifications | — |
| analytics-consumer | http://localhost:4103/stats | — |
| loyalty-consumer | http://localhost:4104/points | — |

### Stop and reset

```bash
docker compose down
docker compose down -v   # wipe order-db + inventory-db volumes
```

---

## Hosting platforms (free → paid)

### Tier 1 — Free / learning

| Platform | Component | Notes |
|----------|-----------|-------|
| **CloudAMQP** | RabbitMQ | Free "Little Lemur" plan — enough for demos, one topic exchange + 4 queues |
| **Neon / Supabase** | `order-db`, `inventory-db` | Two free Postgres instances (or two databases on one instance) |
| **Fly.io / Render** | `order-api` + 4 consumers as separate services | Five independent Docker images from this repo's five `Dockerfile`s |
| **Oracle Cloud VM** | Full `docker compose` | Free ARM instance runs everything |

**Free combo:** CloudAMQP + Neon (×2 databases) + Fly.io (5 tiny services, one per app).

### Tier 2 — Hobby ($10–50/mo)

| Platform | Pattern | Est. cost |
|----------|---------|-----------|
| **DigitalOcean / Hetzner VPS** | `docker compose up -d` running all 7 containers | ~$12–20/mo |
| **Railway** | 5 app services + 2 Postgres plugins + RabbitMQ plugin | ~$25–45/mo |
| **Render** | 5 web services (`order-api` + 4 consumers) + 2 Postgres + CloudAMQP | ~$30–50/mo |

**Key decision:** each consumer needs a **long-lived process** holding an open AMQP connection
(Spring AMQP's `SimpleMessageListenerContainer`, wired by `@RabbitListener`) — this rules out
plain request-driven serverless (vanilla AWS Lambda) for the consumers unless you introduce an
event-source adapter (see Tier 3).

### Tier 3 — Production ($50–500+/mo)

| Component | Managed options |
|-----------|-----------------|
| **Message broker** | Amazon MQ (RabbitMQ), CloudAMQP dedicated, self-hosted on K8s |
| **Consumer processes** | ECS Fargate (one service per consumer), Kubernetes Deployments (one per consumer, independently scalable) |
| **Databases** | RDS (`order-db`, `inventory-db` as separate instances or separate databases) |
| **Producer** | ECS Fargate / Cloud Run, behind an ALB |

**AWS production pattern:**

```
ALB → ECS (order-api tasks)
        ↓ publish
     Amazon MQ (RabbitMQ, topic exchange grocery_events)
        ↓ consume (4 independent bindings)
     ECS (inventory-consumer tasks)  → RDS (inventory-db)
     ECS (notification-consumer tasks)
     ECS (analytics-consumer tasks)
     ECS (loyalty-consumer tasks)
     RDS (order-db, used only by order-api)
```

Each consumer's ECS service scales independently — `loyalty-consumer` getting more traffic than
`analytics-consumer` doesn't require touching the other's task count, because they're on separate
queues with separate consumers, not competing for the same queue's messages.

### Tier 4 — Scale

| Need | Solution |
|------|----------|
| A fifth/sixth consumer added later | New service, new queue binding, zero changes to `order-api` or any existing consumer — see README "Adding loyalty-consumer on day 2" |
| High fan-out volume | RabbitMQ cluster (mirrored/quorum queues), or migrate the exchange to Kafka topics with per-consumer consumer groups if replay-from-history becomes a requirement (see `03-async-queue-processing`'s Kafka comparison for that trade-off) |
| Exactly-once side effects | Idempotent consumers + dedup keys — already implemented for `loyalty-consumer` in this repo (`processedEventIds`); the same pattern generalizes to a `UNIQUE(event_id)` constraint in a real database |
| Observability | Broker metrics (queue depth per consumer), OpenTelemetry traces across `POST /orders` → publish → each consumer's handler |

---

## Per-component production mapping

| Local | Production | Env vars |
|-------|------------|----------|
| `order-api` | Container / PaaS web service | `ORDER_API_PORT`, `ORDER_DB_*`, `RABBITMQ_URL` |
| `inventory-consumer` | Separate container fleet, own scaling | `INVENTORY_CONSUMER_PORT`, `INVENTORY_DB_*`, `RABBITMQ_URL` |
| `notification-consumer` | Separate container fleet, own scaling | `NOTIFICATION_CONSUMER_PORT`, `RABBITMQ_URL` |
| `analytics-consumer` | Separate container fleet, own scaling | `ANALYTICS_CONSUMER_PORT`, `RABBITMQ_URL` |
| `loyalty-consumer` | Separate container fleet, own scaling | `LOYALTY_CONSUMER_PORT`, `RABBITMQ_URL` |
| `order-db` | RDS, Neon, etc. | `ORDER_DB_HOST`, etc. |
| `inventory-db` | RDS, Neon, etc. | `INVENTORY_DB_HOST`, etc. |
| `rabbitmq` | Amazon MQ, CloudAMQP | `RABBITMQ_URL` (`amqps://` in prod) |

**Autoscaling signal:** per-queue RabbitMQ depth (`inventory.order-placed.queue`,
`notification.order-placed.queue`, etc., each independently) → scale that consumer's replica
count. Because each consumer owns its own queue, one consumer falling behind never blocks or
throttles another.

---

## Additional tools for production

### Message broker operations

| Tool | Purpose |
|------|---------|
| RabbitMQ Management / Prometheus plugin | Per-queue depth, publish/consume rates |
| Alerting on queue depth | Catch a stalled consumer before its queue backs up unboundedly |
| [Spring AMQP](https://spring.io/projects/spring-amqp) container metrics | Consumer ack rates, channel errors, per-`@RabbitListener` throughput |

### CI/CD

| Tool | Purpose |
|------|---------|
| GitHub Actions | `mvn test` → `mvn clean package` per app → build 5 independent images |
| Docker Compose (staging) | Mirror prod topology (7 containers) |

Example deploy matrix (5 independent services from 5 independent Dockerfiles):

```yaml
# Pseudocode
order-api:
  build: ./order-api
inventory-consumer:
  build: ./inventory-consumer
  replicas: 2
notification-consumer:
  build: ./notification-consumer
  replicas: 1
analytics-consumer:
  build: ./analytics-consumer
  replicas: 1
loyalty-consumer:
  build: ./loyalty-consumer
  replicas: 2
```

### Observability

| Tool | Purpose |
|------|---------|
| Sentry | Exceptions across all 5 apps |
| OpenTelemetry + Micrometer | Trace `order.placed` from `POST /orders` → exchange → each consumer's handler, correlated by `eventId` |
| Grafana | Per-queue depth, per-consumer throughput/lag |

### Security

| Tool | Purpose |
|------|---------|
| TLS on AMQP (`amqps://`) | Encrypt broker traffic in production |
| VPC private subnets | Apps talk to RDS/Amazon MQ without public exposure |
| Secrets Manager | `RABBITMQ_URL`, DB credentials, one secret set per environment |

---

## Environment variables (production checklist)

| Variable | Required by | Notes |
|----------|-------------|-------|
| `ORDER_API_PORT` | `order-api` | |
| `ORDER_DB_*` | `order-api` only | No other app touches `order-db` |
| `INVENTORY_DB_*` | `inventory-consumer` only | No other app touches `inventory-db` |
| `RABBITMQ_URL` | All 5 apps | Same broker, `amqps://` in production |
| `INVENTORY_CONSUMER_PORT` / `NOTIFICATION_CONSUMER_PORT` / `ANALYTICS_CONSUMER_PORT` / `LOYALTY_CONSUMER_PORT` | Each respective consumer | Inspection-endpoint port only — not used for inter-app calls |

---

## Consumer scaling cheat sheet

| Command | Effect |
|---------|--------|
| `docker compose up -d --scale inventory-consumer=3` | 3 competing consumers on `inventory.order-placed.queue` — still only inventory-consumer's job, just distributed across more workers |
| K8s `replicas: 3` on the `inventory-consumer` Deployment | Same effect in production |
| Scaling `loyalty-consumer` independently of `inventory-consumer` | Fully supported — they're separate queues, separate consumer groups in every sense that matters here |

RabbitMQ distributes each queue's messages across however many consumers are bound to *that*
queue — scaling one consumer app never touches another's message flow, because they're not
sharing a queue (see README's contrast table with `03-async-queue-processing`, where scaling
`worker` *does* affect how fast a single shared queue drains).

---

## Cost estimate (rough monthly)

| Scenario | Est. cost |
|----------|-----------|
| Local Docker | $0 |
| CloudAMQP free + Neon free (×2) + Fly.io (5 tiny services) | $0 |
| VPS full stack (7 containers) | ~$15–20 |
| Amazon MQ + 2× RDS + 5 ECS services | ~$200–350 |

---

## Related docs

- [README.md](./README.md) — fan-out topology, idempotency proof, publish-after-commit, contrast
  with `03-async-queue-processing`
- [../03-async-queue-processing/HOSTING.md](../03-async-queue-processing/HOSTING.md) — RabbitMQ
  retry/DLQ hosting notes (point-to-point queueing, not fan-out)
- [../01-modular-monolith/HOSTING.md](../01-modular-monolith/HOSTING.md) — RabbitMQ domain event
  bus inside a single monolith, for comparison with this project's fully-separate-processes
  approach
- [../../system-design/09-event-driven-architecture/HOSTING.md](../../system-design/09-event-driven-architecture/HOSTING.md) — TypeScript/NestJS edition
