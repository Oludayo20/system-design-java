# Hosting & Deployment Guide — BlogStack Monolithic Architecture (Java / Spring Boot)

This document covers how to run BlogStack locally (including Docker) and where to host it in
production. This project is a **single Spring Boot API backed by one PostgreSQL database** — no
Redis, no message broker, no second deployable unit. That's the point of the pattern: everything
in this repo ships and scales together.

For architecture, the tradeoffs this project demonstrates, and the curl walkthrough, see
[README.md](./README.md).

---

## Application components

| Component | Role | Required? | Local (Docker) service |
|-----------|------|-----------|-------------------------|
| **Spring Boot API** | Auth, Users, Posts, Comments, Notifications — all five modules in one process | Yes | `api` |
| **PostgreSQL 16** | Single shared database; every module's table lives in `public`, with real cross-module foreign keys | Yes | `postgres` |
| **Flyway migrations** | Schema management (`spring.flyway.enabled=true`; Hibernate `ddl-auto: none`) | Yes | Runs automatically on API startup |

There is no separate frontend, cache, queue, or load balancer in this project — a single API
instance plus its database is the full deployable unit.

**Stack:** Spring Boot 3.3 / Java 21 / Maven / Spring Data JPA / springdoc-openapi (Swagger UI at
`/docs`).

---

## Prerequisites (your machine)

| Tool | Version | Purpose |
|------|---------|---------|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Latest | Run Postgres and the API in containers |
| [Docker Compose](https://docs.docker.com/compose/) | v2+ (bundled with Docker Desktop) | Orchestrate the stack |
| [Java (Temurin)](https://adoptium.net/) | 21 | Local dev without containerizing the API |
| [Maven](https://maven.apache.org/) | 3.9+ | Build, test, run (`mvn spring-boot:run`) |
| [curl](https://curl.se/) or [HTTPie](https://httpie.io/) | Any | Test endpoints |
| [Git](https://git-scm.com/) | Any | Clone and deploy |

Optional but useful:

| Tool | Purpose |
|------|---------|
| [jq](https://jqlang.github.io/jq/) | Parse JSON in shell scripts |
| [Postman](https://www.postman.com/) / [Insomnia](https://insomnia.rest/) | Interactive API testing |
| [DBeaver](https://dbeaver.io/) / [pgAdmin](https://www.pgadmin.org/) | Inspect PostgreSQL tables |

---

## Run locally with Docker

### 1. Clone and configure

```bash
cd system-design-java/07-monolithic-architecture
cp .env.example .env
```

Review `.env` — defaults match `docker-compose.yml`. Change `JWT_SECRET` before any shared use.

### 2. Start the full stack

```bash
docker compose up --build
```

This command:

1. Pulls `postgres:16-alpine`
2. Builds the Spring Boot API image from `Dockerfile` (`mvn clean package` in the build stage)
3. Waits for the Postgres health check
4. Starts the API — **Flyway runs automatically on boot** (`spring.flyway.enabled=true`)

**First run** may take a few minutes (image pulls + Maven dependency download in the build stage).

### 3. Verify services

| Service | URL / Port | Default credentials |
|---------|------------|----------------------|
| API + Swagger | http://localhost:3007/docs | — |
| PostgreSQL | `localhost:5432` | `blogstack` / `blogstack_dev_password` / db `blogstack` |

```bash
# Quick health check
curl -s http://localhost:3007/posts

# Follow API logs
docker compose logs -f api
```

### 4. Stop and reset

```bash
# Stop containers, keep data
docker compose down

# Stop and delete volumes (fresh database)
docker compose down -v
```

### Option B — Postgres in Docker, API on the host (hot reload)

Useful when editing Java and you want fast restarts:

```bash
cp .env.example .env
set -a && source .env && set +a   # export env vars for Spring Boot

docker compose up -d postgres
mvn spring-boot:run
```

`.env.example` already points `POSTGRES_HOST` at `localhost`. Flyway migrations run automatically
on first `mvn spring-boot:run` — no separate migration command needed.

### Build and run the JAR directly

```bash
mvn clean package
set -a && source .env && set +a
java -jar target/monolithic-architecture.jar
```

### Build Docker image manually

```bash
mvn clean package -DskipTests
docker build -t blogstack-api .
docker run -p 3007:3007 --env-file .env blogstack-api
```

### Troubleshooting Docker

| Problem | Fix |
|---------|-----|
| Port 5432/3007 already in use | Stop conflicting services or change host ports in `docker-compose.yml` |
| `api` exits on Flyway error | `docker compose logs api`; ensure Postgres is healthy |
| Docker daemon not running | Start Docker Desktop |
| Maven build fails locally | Ensure Java 21: `java -version` |

---

## Hosting platforms (free → paid)

Platforms are listed in approximate cost order. Because this app has only two moving pieces (API +
Postgres), it's one of the cheaper projects in the series to host.

### Tier 1 — Free / learning & demos

| Platform | What to host | Limits / notes |
|----------|--------------|-----------------|
| **[Fly.io](https://fly.io/)** | API container | Free allowance; good for one small VM + volume |
| **[Render](https://render.com/)** | API (free web service) | Spins down after inactivity; cold starts |
| **[Railway](https://railway.app/)** | API + Postgres add-on | Trial credits; easy Docker deploy |
| **[Neon](https://neon.tech/)** | PostgreSQL | Free serverless Postgres; replace `postgres` service |
| **[Oracle Cloud Always Free](https://www.oracle.com/cloud/free/)** | Full stack on one ARM VM | Run `docker compose` on a free Ampere instance |

**Recommended free combo:** Neon (Postgres) + Fly.io or Render (API). Set the `POSTGRES_*`
connection variables to the managed connection string instead of the self-hosted container.

### Tier 2 — Hobby / small production ($5–20/mo)

| Platform | Best for | Typical cost |
|----------|----------|---------------|
| **[DigitalOcean Droplet](https://www.digitalocean.com/products/droplet)** | Single VM running full `docker compose` | ~$6/mo |
| **[Hetzner Cloud](https://www.hetzner.com/cloud)** | Same as DO; strong price/performance in EU | ~€4/mo |
| **[Railway](https://railway.app/)** | Managed Postgres + API from GitHub | ~$5–15/mo |
| **[Render](https://render.com/)** | Managed Postgres + web service (no cold start on paid) | ~$7–14/mo |

**Recommended hobby stack:** One small VPS with Docker Compose (simplest ops) **or**
Railway/Render with a managed database (less ops, slightly higher cost).

### Tier 3 — Production context

This demo is intentionally minimal. In a real setting you would likely not deploy BlogStack
standalone — you'd either split its modules (see the "downside made concrete" section in
[README.md](./README.md)) or run it behind the same infrastructure the rest of the series uses:

| Pattern | Where it lives |
|---------|-----------------|
| Enforced module boundaries + domain events | `01-modular-monolith` |
| Retry/circuit breaker around a synchronous dependency (e.g. `comments -> notifications`) | `05-resilience` |
| Multi-replica availability | `04-ecom-marketplace-capstone` (Nginx + 2 API instances) |

### Tier 4 — Enterprise

- Managed Postgres with read replicas (RDS Multi-AZ, Cloud SQL HA)
- Service mesh / API gateway in front of the monolith while it's incrementally decomposed
- Strangler-fig migration of `comments` or `notifications` into their own services

---

## Per-component production mapping

| Local service | Managed alternative (free → paid) | Connection env var |
|---------------|-------------------------------------|----------------------|
| `postgres` | Neon → Supabase → RDS / Cloud SQL | `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` |
| `api` | Fly.io → Render → ECS / Cloud Run / K8s | `PORT`, `JWT_SECRET`, `JWT_EXPIRES_IN` |

---

## Additional tools for a functional production system

These are not in the repo but are expected in real deployments:

### CI/CD

| Tool | Purpose |
|------|---------|
| [GitHub Actions](https://github.com/features/actions) | `mvn test` → `mvn clean package` → build Docker image → deploy |

Example pipeline: `mvn test` → `mvn clean package -DskipTests` → `docker build` → push to
registry → deploy. Flyway migrations run on container startup.

### Secrets & config

| Tool | Purpose |
|------|---------|
| [Doppler](https://www.doppler.com/) | Centralized secrets |
| [AWS Secrets Manager](https://aws.amazon.com/secrets-manager/) | Cloud-native secrets |

Never commit a production `JWT_SECRET` or database password.

### Monitoring & alerting

| Tool | Purpose |
|------|---------|
| [Sentry](https://sentry.io/) | Error tracking (Java SDK available) |
| [Micrometer](https://micrometer.io/) + [Prometheus](https://prometheus.io/) + [Grafana](https://grafana.com/) | Spring Boot metrics and dashboards |
| [UptimeRobot](https://uptimerobot.com/) | Free uptime checks |

### Database operations

| Tool | Purpose |
|------|---------|
| [pg_dump](https://www.postgresql.org/docs/current/app-pgdump.html) | Backups |
| [Flyway](https://flywaydb.org/) | Migrations (runs on startup; versioned SQL in `src/main/resources/db/migration/`) |

---

## Environment variables (production checklist)

| Variable | Required | Notes |
|----------|----------|-------|
| `PORT` | Yes | Usually `3007` or platform-assigned |
| `POSTGRES_*` | Yes | Use managed DB host in cloud |
| `JWT_SECRET` | Yes | Long random string; rotate with care |
| `JWT_EXPIRES_IN` | No | Default `1h` |

---

## Deployment workflow (summary)

1. **Build:** `mvn clean package` → `docker build -t blogstack-api .`
2. **Push:** Tag and push to ECR, GCR, Docker Hub, or GHCR
3. **Migrate:** Flyway runs on startup (`java -jar target/monolithic-architecture.jar`); for
   zero-downtime deploys, run migrations as a separate job first
4. **Deploy:** Start API with env vars pointing at managed Postgres
5. **Verify:** `GET /posts`, register + login + create a post + comment, confirm
   `GET /notifications/me` shows the notification row
6. **Monitor:** Error rate, response latency, DB connection pool saturation

---

## Cost estimate (rough monthly)

| Scenario | Components | Est. cost |
|----------|------------|-----------|
| Local dev | Docker on laptop | $0 |
| Free cloud demo | Neon + Fly.io/Render free | $0 |
| Hobby VPS | 1× 1–2GB Droplet, full compose | ~$6 |
| Small production | Managed Postgres + 1–2 API instances | ~$25–60 |

---

## Related docs

- [README.md](./README.md) — architecture, curl walkthrough
- [../README.md](../README.md) — full system design series index (Java edition)
- [../01-modular-monolith/HOSTING.md](../01-modular-monolith/HOSTING.md) — the enforced-boundaries version of this same "one deploy, one process" shape
- [../05-resilience/HOSTING.md](../05-resilience/HOSTING.md) — smallest-footprint project in the series; this one follows the same API-only-plus-one-datastore pattern
- [../../system-design/07-monolithic-architecture/HOSTING.md](../../system-design/07-monolithic-architecture/HOSTING.md) — TypeScript/NestJS edition
