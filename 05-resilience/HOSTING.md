# Hosting & Deployment Guide — Resilience Demo (Java / Spring Boot)

This document covers how to run the resilience patterns demo locally (including Docker) and where to host it in production. This project is a **single stateless Spring Boot API** with an in-memory flaky payment simulator — no database or message broker required.

For retry, circuit breaker, and fallback behavior, see [README.md](./README.md).

---

## Application components

| Component | Role | Required? | Notes |
|-----------|------|-----------|-------|
| **Spring Boot API** | `POST /checkout`, `GET /checkout/circuit` | Yes | Port `3005` by default |
| **In-memory payment gateway** | Simulates flaky Paystack + Flutterwave fallback | Yes | No external API keys |
| **Circuit breaker** | CLOSED → OPEN → HALF_OPEN state machine | Yes | In-process |
| **Retry logic** | Application-level retries with delay | Yes | Configurable via `.env` / `application.properties` |

No PostgreSQL, Redis, RabbitMQ, or load balancer — this is intentionally minimal so you can focus on application-boundary resilience.

**Stack:** Spring Boot 3.3 / Java 21 / Maven / springdoc-openapi at `/docs`. Production patterns map to [Resilience4j](https://github.com/resilience4j/resilience4j) when integrated into real services.

---

## Prerequisites (your machine)

| Tool | Version | Purpose |
|------|---------|---------|
| Java (Temurin) | 21 | Run API |
| Maven | 3.9+ | Build, test, run |
| Docker Desktop | Latest | Optional — containerized run via `Dockerfile` + `docker-compose.yml` |
| curl | Any | Exercise checkout endpoint |

---

## Run locally with Docker

This project includes a **`Dockerfile`** and **`docker-compose.yml`** for a one-command containerized run.

### Quick start

```bash
cd system-design-java/05-resilience
cp .env.example .env
docker compose up --build
```

The `Dockerfile` multi-stage build:

1. **Build stage:** `maven:3.9-eclipse-temurin-21` → `mvn clean package -DskipTests`
2. **Runtime stage:** `eclipse-temurin:21-jre` → `java -jar app.jar` (from `target/resilience.jar`)

API available at:

| Endpoint | URL |
|----------|-----|
| Swagger | http://localhost:3005/docs |
| Checkout | `POST http://localhost:3005/checkout` |
| Circuit state | `GET http://localhost:3005/checkout/circuit` |

### Try it

```bash
# Run checkout several times — observe retries, circuit open, Flutterwave fallback
curl -X POST http://localhost:3005/checkout \
  -H 'Content-Type: application/json' \
  -d '{"amount": 5000}'

curl http://localhost:3005/checkout/circuit
```

### Tune behavior in `.env`

Docker Compose passes these to the container. For local `mvn spring-boot:run`, export them or set in `application.properties`:

| Variable | Default | Purpose |
|----------|---------|---------|
| `PORT` | `3005` | HTTP port |
| `PAYMENT_FAILURE_RATE` | `0.7` | How often primary provider "fails" |
| `CIRCUIT_FAILURE_THRESHOLD` | `5` | Failures before circuit opens |
| `CIRCUIT_RESET_MS` | `10000` | Time before half-open probe |
| `MAX_RETRIES` | `3` | Retries per checkout attempt |
| `RETRY_DELAY_MS` | `200` | Delay between retries |

### Run without Docker (hot reload)

```bash
cp .env.example .env
set -a && source .env && set +a
mvn spring-boot:run
```

### Build and run the JAR

```bash
mvn clean package
set -a && source .env && set +a
java -jar target/resilience.jar
```

### Build Docker image manually

```bash
mvn clean package -DskipTests
docker build -t resilience-api .
docker run -p 3005:3005 --env-file .env resilience-api
```

### Stop

```bash
docker compose down
```

---

## Hosting platforms (free → paid)

Because this app is stateless and has no external dependencies, it is the **cheapest project in the series to host**.

### Tier 1 — Free

| Platform | Notes |
|----------|-------|
| **[Render](https://render.com/)** free web service | Spins down when idle |
| **[Fly.io](https://fly.io/)** | Free allowance; deploy `Dockerfile` |
| **[Railway](https://railway.app/)** | Trial credits |
| **[Google Cloud Run](https://cloud.google.com/run)** | Container from `Dockerfile`; scale to zero |

**Recommended free:** Fly.io or Render — push Docker image, set env vars, done.

### Tier 2 — Hobby ($0–7/mo)

| Platform | Est. cost |
|----------|-----------|
| Render paid starter | ~$7/mo (no cold start) |
| Fly.io shared CPU | ~$3–5/mo |
| DigitalOcean App Platform | ~$5/mo |

### Tier 3 — Production context

This demo is **not** a standalone production service — it teaches patterns you embed in real apps (e.g. the Oja capstone checkout flow). In production:

| Pattern | Where it lives |
|---------|----------------|
| Retry + circuit breaker on Paystack | Payment module inside `04-ecom-marketplace-capstone` API |
| Broker-level retry/DLQ | `03-async-queue-processing` RabbitMQ topology |
| Multi-replica availability | `04` Nginx + 2 API instances |

Host the **parent application** (modular monolith), not this demo alone.

### Tier 4 — Enterprise

- Service mesh circuit breaking (Istio, Linkerd)
- [Resilience4j](https://github.com/resilience4j/resilience4j) policies (native Java; this demo uses hand-rolled logic for teaching)
- Multi-provider payment routing with observability (Datadog APM)

---

## Tools needed for a functional deployment

### Required (this project)

| Tool | Purpose |
|------|---------|
| Java 21 or Docker | Runtime |
| `.env` | Tune failure rates for demos |
| `Dockerfile` + `docker-compose.yml` | Containerized deploy |

### Required when integrating into Oja/production

| Tool | Purpose |
|------|---------|
| **Real payment SDKs** | Paystack, Flutterwave official APIs |
| **Secrets Manager** | API keys — never in `.env` in prod |
| **PostgreSQL** | Persist checkout/payment state |
| **RabbitMQ / SQS** | Queue failed payments for background retry |
| **Idempotency keys** | Prevent double charges on retry |

### CI/CD

| Tool | Purpose |
|------|---------|
| GitHub Actions | `mvn test` → `mvn clean package` → `docker build` → deploy |
| Unit tests | `mvn test` — circuit breaker specs need no server |

Example pipeline:

```bash
mvn test
mvn clean package -DskipTests
docker build -t resilience-api .
# push + deploy
```

### Observability

| Tool | Purpose |
|------|---------|
| Structured logging (Logback / JSON) | Log circuit state transitions |
| Micrometer metrics | `checkout_success_total`, `circuit_open_total` |
| Sentry | Capture unhandled payment errors |
| PagerDuty | Alert when circuit stays OPEN > N minutes |

### Load testing

| Tool | Purpose |
|------|---------|
| k6 / hey | Hammer `/checkout` to force circuit open |
| Chaos engineering | [Gremlin](https://www.gremlin.com/), [Litmus](https://litmuschaos.io/) — inject provider failures |

---

## Environment variables (production checklist)

When porting patterns to a real payment service:

| Variable | Purpose |
|----------|---------|
| `PAYSTACK_SECRET_KEY` | Real provider (secrets manager) |
| `FLUTTERWAVE_SECRET_KEY` | Fallback provider |
| `CIRCUIT_FAILURE_THRESHOLD` | Tune per provider SLO |
| `MAX_RETRIES` | Only for idempotent operations |
| `WEBHOOK_SECRET` | Verify provider callbacks |

---

## Mapping to full stack hosting

| This demo | In Oja capstone (`04`) |
|-----------|------------------------|
| In-memory Paystack | Replace with HTTP client + circuit breaker wrapper (or Resilience4j) |
| Single instance | 2+ replicas behind Nginx; circuit breaker is **per process** — use shared state (Redis) for cluster-wide circuit if needed |
| Queued fallback response | Publish to RabbitMQ `payment.retry` queue (`03` pattern) |

---

## Cost estimate

| Scenario | Est. cost |
|----------|-----------|
| Local / Docker | $0 |
| Fly.io / Render free | $0 |
| Paid always-on hobby | ~$5–7/mo |

---

## Related docs

- [README.md](./README.md) — pattern explanation
- [../LEVELS-6-7.md](../LEVELS-6-7.md) — Level 6 resilience concepts
- [../04-ecom-marketplace-capstone/HOSTING.md](../04-ecom-marketplace-capstone/HOSTING.md) — where to deploy real checkout
- [../03-async-queue-processing/HOSTING.md](../03-async-queue-processing/HOSTING.md) — async retry via message broker
- [../../system-design/05-resilience/HOSTING.md](../../system-design/05-resilience/HOSTING.md) — TypeScript/NestJS edition
