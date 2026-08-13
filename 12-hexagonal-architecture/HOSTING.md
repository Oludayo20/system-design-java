# Hosting & Deployment Guide — Orbit (Hexagonal Architecture, Java / Spring Boot)

This document covers everything you need to run Orbit locally (with Docker) and deploy it to
production: which platforms to use, what each component needs, and recommended tooling from free
tiers through paid production stacks.

For architecture, business rules, and API/CLI walkthroughs, see [README.md](./README.md).

---

## Application components

| Component | Role in this project | Required? | Local (Docker) service |
|-----------|---------------------|-----------|-------------------------|
| **Spring Boot API** | HTTP inbound adapter — `POST /subscriptions`, `POST /subscriptions/{id}/change-plan`, `POST /subscriptions/{id}/cancel`, `GET /subscriptions/{id}` | Yes | `api` |
| **PostgreSQL 16** | Outbound adapter target when `APP_REPOSITORY=postgres` — one `subscriptions` table | Only when `APP_REPOSITORY=postgres` (the Docker default) | `postgres` |
| **In-memory repository** | Outbound adapter used when `APP_REPOSITORY=memory` — no external service at all | Alternative to Postgres | n/a (in-process) |
| **Stripe/Flutterwave mock gateways** | Outbound adapters, both simulated in-process — no real network calls, no API keys | Yes (one or the other, via `APP_PAYMENT_PROVIDER`) | n/a (in-process) |
| **Flyway migrations** | Schema management for the `subscriptions` table (`spring.flyway.enabled=true`, `ddl-auto: none`) | Only when `APP_REPOSITORY=postgres` | Runs automatically on API container startup |
| **Orbit CLI** | Second inbound adapter (`mvn spring-boot:run -Dspring-boot.run.profiles=cli`), drives the same use-case beans as the API | Optional — a demo/ops tool, not a deployable service | Run on host, or `docker compose exec`/a one-off container |

There is no Redis, message broker, or load balancer in this project — the entire point is the
core/adapter split, not extra infrastructure.

**Stack:** Spring Boot 3.3 / Java 21 / Maven / Spring Data JPA / springdoc-openapi (Swagger UI at
`/docs`).

---

## Prerequisites (your machine)

| Tool | Version | Purpose |
|------|---------|---------|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | Latest | Run Postgres and the API in containers |
| [Docker Compose](https://docs.docker.com/compose/) | v2+ (bundled with Docker Desktop) | Orchestrate the stack |
| [Java (Temurin)](https://adoptium.net/) | 21 | Local dev without containerizing the API, and to run the CLI |
| [Maven](https://maven.apache.org/) | 3.9+ | Build, test, run (`mvn spring-boot:run`) |
| [curl](https://curl.se/) or [HTTPie](https://httpie.io/) | Any | Test endpoints |
| [Git](https://git-scm.com/) | Any | Clone and deploy |

Optional but useful:

| Tool | Purpose |
|------|---------|
| [jq](https://jqlang.github.io/jq/) | Parse JSON in shell scripts |
| [Postman](https://www.postman.com/) / [Insomnia](https://insomnia.rest/) | Interactive API testing |
| [DBeaver](https://dbeaver.io/) / [pgAdmin](https://www.pgadmin.org/) | Inspect the `subscriptions` table |

---

## Run locally with Docker

### 1. Clone and configure

```bash
cd system-design-java/12-hexagonal-architecture
```

`docker-compose.yml` defaults `APP_REPOSITORY=postgres` and `APP_PAYMENT_PROVIDER=stripe` for the
`api` service (Postgres is always available in Compose) — override either by exporting the env var
before `docker compose up`, e.g. `APP_PAYMENT_PROVIDER=flutterwave docker compose up --build -d`.

Only copy `.env.example` to `.env` for the **host-side** `mvn spring-boot:run` workflow (Option B
below) — `docker-compose.yml` already supplies every env var the `api`/`postgres` services need
with sensible defaults baked in. If you do keep a `.env` file in this directory, note that Docker
Compose auto-loads it for variable substitution too, so a `.env` with `APP_REPOSITORY=memory` will
override the compose file's own `postgres` default. Delete `.env` (or export
`APP_REPOSITORY=postgres` explicitly) before `docker compose up` if you want Postgres for sure.

### 2. Start the stack

```bash
docker compose up --build -d
```

This command:

1. Pulls `postgres:16-alpine`
2. Builds the Spring Boot API image from `Dockerfile` (`mvn clean package` in the build stage)
3. Waits for Postgres's health check
4. Starts the API — **Flyway runs automatically on boot** when `APP_REPOSITORY=postgres`
   (`spring.flyway.enabled=true` under the `postgres` Spring profile, activated by
   `OrbitApplication`'s `main()` from the same env var)

**First run** may take 2–5 minutes (image pull + Maven dependency download in the build stage).

### 3. Verify services

| Service | URL / Port | Default credentials |
|---------|------------|----------------------|
| API + Swagger | http://localhost:3012/docs | — |
| PostgreSQL | `localhost:5432` | `orbit` / `orbit_dev_password` / db `orbit` |

```bash
# Quick health check
curl -s http://localhost:3012/docs | head -c 200

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

Useful when editing Java and you want fast restart:

```bash
cp .env.example .env
docker compose up -d postgres
# in .env: APP_REPOSITORY=postgres, POSTGRES_HOST=localhost
set -a && source .env && set +a
mvn spring-boot:run
```

Flyway migrations run automatically on first `mvn spring-boot:run` — no separate migration command
needed.

### Run fully standalone (no Docker at all)

`APP_REPOSITORY=memory` needs no database whatsoever:

```bash
cp .env.example .env   # APP_REPOSITORY=memory by default
mvn spring-boot:run
```

### Run the CLI

```bash
# Standalone (in-memory, no DB):
mvn spring-boot:run -Dspring-boot.run.profiles=cli \
    -Dspring-boot.run.arguments="subscribe --customer=c1 --plan=pro"

# Against the same Postgres the Docker API uses:
APP_REPOSITORY=postgres POSTGRES_HOST=localhost POSTGRES_PORT=5432 \
POSTGRES_USER=orbit POSTGRES_PASSWORD=orbit_dev_password POSTGRES_DB=orbit \
mvn spring-boot:run -Dspring-boot.run.profiles=cli,postgres \
    -Dspring-boot.run.arguments="get --id=<id>"
```

See [README.md](./README.md#proof-cli-and-http-both-drive-the-same-core) for why both
`APP_REPOSITORY=postgres` and the `postgres` Spring profile need to be set together.

### Troubleshooting Docker

| Problem | Fix |
|---------|-----|
| Port 5432/3012 already in use | Stop conflicting services or change host ports in `docker-compose.yml` |
| `api` exits on Flyway/migration error | `docker compose logs api`; ensure Postgres is healthy |
| `api` container uses in-memory unexpectedly | Check for a stray `.env` with `APP_REPOSITORY=memory` in this directory — Compose loads it and it overrides the compose file's own default (see step 1 above) |
| Docker daemon not running | Start Docker Desktop |
| Maven build fails locally | Ensure Java 21: `java -version` |

---

## Hosting platforms (free → paid)

Platforms are listed in approximate cost order.

### Tier 1 — Free / learning & demos

| Platform | What to host | Limits / notes |
|----------|---------------|-----------------|
| **[Fly.io](https://fly.io/)** | API container | Free allowance; good for one small VM + volume |
| **[Render](https://render.com/)** | API (free web service) | Spins down after inactivity; cold starts |
| **[Railway](https://railway.app/)** | API + Postgres add-on | Trial credits; easy Docker deploy |
| **[Neon](https://neon.tech/)** | PostgreSQL | Free serverless Postgres; replace `postgres` service |
| **[Supabase](https://supabase.com/)** | PostgreSQL | Free tier Postgres alternative to Neon |

**Recommended free combo:** Neon (Postgres) + Fly.io or Render (API). Or skip Postgres entirely and
demo with `APP_REPOSITORY=memory` — this project is one of the cheapest in the series to run
because the in-memory adapter is a legitimate, fully-functional alternative, not a stub.

### Tier 2 — Hobby / small production ($5–50/mo)

| Platform | Best for | Typical cost |
|----------|----------|----------------|
| **[DigitalOcean Droplet](https://www.digitalocean.com/products/droplet)** | Single VM running full `docker compose` | ~$6–12/mo |
| **[Hetzner Cloud](https://www.hetzner.com/cloud)** | Same as DO; strong price/performance in EU | ~€4–10/mo |
| **[Railway](https://railway.app/)** | Managed Postgres + API from GitHub | ~$5–20/mo |
| **[Render](https://render.com/)** | Managed Postgres + web service (no cold start on paid) | ~$7–25/mo |

### Tier 3 — Production / growth ($50–500+/mo)

| Platform | Components | Notes |
|----------|------------|-------|
| **[AWS](https://aws.amazon.com/)** | ECS Fargate or EKS (API), RDS PostgreSQL | Use Secrets Manager for real payment provider keys if this ever talks to a live gateway |
| **[Google Cloud](https://cloud.google.com/)** | Cloud Run, Cloud SQL | Good autoscaling on Cloud Run |
| **[DigitalOcean](https://www.digitalocean.com/)** | App Platform or DOKS, Managed Postgres | Simpler than the big three clouds |

**Recommended production stack (AWS example):**

```
Internet → ALB → ECS Fargate (API, 2+ tasks)
              → RDS PostgreSQL (Multi-AZ)
Secrets: AWS Secrets Manager (real payment provider keys, DB password)
Logs: CloudWatch
```

### Tier 4 — Scale / enterprise ($500+/mo)

| Need | Options |
|------|---------|
| Multi-region API | Cloudflare + regional ECS/GKE clusters |
| Read replicas | RDS read replicas if subscription reads dominate |
| Real payment providers | Swap `StripeMockAdapter`/`FlutterwaveMockAdapter` for real SDK-backed adapters implementing the same `PaymentGatewayPort` — no core changes needed |
| Observability | Datadog, New Relic, Grafana Cloud |

---

## Per-component production mapping

| Local service | Managed alternative (free → paid) | Connection env var |
|----------------|-------------------------------------|----------------------|
| `postgres` | Neon → Supabase → RDS / Cloud SQL | `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` |
| `api` | Fly.io → Render → ECS / Cloud Run / K8s | `PORT`, `APP_REPOSITORY`, `APP_PAYMENT_PROVIDER` |
| `StripeMockAdapter` / `FlutterwaveMockAdapter` | Real Stripe/Flutterwave SDK adapter implementing `PaymentGatewayPort` | Provider secret key(s), via Secrets Manager |

---

## Additional tools for a functional production system

These are not in the repo but are expected in real deployments:

### CI/CD

| Tool | Purpose |
|------|---------|
| [GitHub Actions](https://github.com/features/actions) | `mvn test` → `mvn clean package` → build Docker image → deploy |
| [GitLab CI](https://about.gitlab.com/stages-devops-lifecycle/continuous-integration/) | Same |

Example pipeline steps: `mvn test` → `mvn clean package -DskipTests` → `docker build` → push to
registry → deploy. Flyway migrations run on container startup (or as a one-off init container
before traffic, for zero-downtime deploys).

### Secrets & config

| Tool | Purpose |
|------|---------|
| [Doppler](https://www.doppler.com/) | Centralized secrets |
| [AWS Secrets Manager](https://aws.amazon.com/secrets-manager/) | Cloud-native secrets |

Never commit production database passwords or real payment provider keys.

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
| `PORT` | Yes | Usually `3012` or platform-assigned |
| `APP_REPOSITORY` | Yes | `postgres` in any real deployment; `memory` loses all data on restart |
| `APP_PAYMENT_PROVIDER` | Yes | `stripe` or `flutterwave` — both are mocked here; a real deployment needs a real-SDK adapter |
| `POSTGRES_*` | Only if `APP_REPOSITORY=postgres` | Use a managed DB host in cloud |

---

## Deployment workflow (summary)

1. **Build:** `mvn clean package` → `docker build -t orbit-api .`
2. **Push:** Tag and push to ECR, GCR, Docker Hub, or GHCR
3. **Migrate:** Flyway runs on startup (`java -jar target/hexagonal-architecture.jar` with
   `APP_REPOSITORY=postgres`); for zero-downtime deploys, run migrations as a separate job first
4. **Deploy:** Start API with `APP_REPOSITORY=postgres` and Postgres connection env vars pointing
   at the managed database
5. **Verify:** `POST /subscriptions`, `GET /subscriptions/{id}`
6. **Monitor:** Error rate, response time

---

## Cost estimate (rough monthly)

| Scenario | Components | Est. cost |
|----------|------------|-----------|
| Local dev (in-memory) | Single JVM process, no DB | $0 |
| Local dev (Docker + Postgres) | Docker on laptop | $0 |
| Free cloud demo | Neon + Fly.io/Render free | $0 |
| Hobby VPS | 1× 1GB Droplet, full compose | ~$6 |
| Small production | Managed Postgres + 2 API instances | ~$40–80 |

---

## Related docs

- [README.md](./README.md) — architecture, concept, curl/CLI walkthrough
- [../README.md](../README.md) — full system design series index (Java edition)
- [../../system-design/12-hexagonal-architecture/HOSTING.md](../../system-design/12-hexagonal-architecture/HOSTING.md) — TypeScript/NestJS edition (same topology)
