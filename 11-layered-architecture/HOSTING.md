# Hosting & Deployment Guide — Riverside Library (Java / Spring Boot)

This document covers everything you need to run Riverside Library locally (with Docker) and
deploy it to production: which platforms to use, what each component needs, and recommended
tooling from free tiers through paid production stacks.

For architecture and API walkthroughs, see [README.md](./README.md).

---

## Application components

| Component | Role in this project | Required? | Local (Docker) service |
|-----------|---------------------|-----------|------------------------|
| **Spring Boot API** | HTTP entrypoint; Books, Members, Loans -- each internally split into Presentation/Application/Domain/Data Access layers | Yes | `api` |
| **PostgreSQL 16** | Primary datastore; single schema (`books`, `members`, `loans` tables) | Yes | `postgres` |
| **Flyway migrations** | Schema management (`spring.flyway.enabled=true`; Hibernate `ddl-auto: none`) | Yes | Runs automatically on API startup |

There is no separate frontend, cache, message broker, or load balancer in this project — a single
API instance backed by a single Postgres database is the full deployable unit. That's the point of
this project: all the internal structure (4 layers) lives inside *one* process, not spread across
infrastructure.

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
| [jq](https://jqlang.github.io/jq/) | Parse JSON in shell scripts (used throughout the README curl walkthrough) |
| [Postman](https://www.postman.com/) / [Insomnia](https://insomnia.rest/) | Interactive API testing |
| [DBeaver](https://dbeaver.io/) / [pgAdmin](https://www.pgadmin.org/) | Inspect PostgreSQL tables |

---

## Run locally with Docker

### 1. Clone and configure

```bash
cd system-design-java/11-layered-architecture
cp .env.example .env
```

Review `.env` — defaults match `docker-compose.yml`. The Postgres host port is mapped to `5433`
(not `5432`) to avoid clashing with other local Postgres containers; this only matters if you run
the API on the host against the Dockerized database (Option B below) — inside `docker compose up`
the `api` service always reaches Postgres over the internal network at `postgres:5432` regardless
of the host port mapping.

### 2. Start the full stack

```bash
docker compose up --build
```

This command:

1. Pulls `postgres:16-alpine`
2. Builds the Spring Boot API image from `Dockerfile` (`mvn clean package` in the build stage)
3. Waits for the Postgres health check
4. Starts the API — **Flyway runs automatically on boot** (`spring.flyway.enabled=true`)

**First run** may take a couple of minutes (image pull + Maven dependency download in the build
stage).

### 3. Verify services

| Service | URL / Port | Default credentials |
|---------|------------|---------------------|
| API + Swagger | http://localhost:3011/docs | — |
| PostgreSQL | `localhost:5433` (host) / `postgres:5432` (in-network) | `library` / `library_dev_password` / db `library` |

```bash
# Quick health check
curl -s http://localhost:3011/books

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

Useful when editing Java and you want fast iterative restarts:

```bash
cp .env.example .env
set -a && source .env && set +a   # export env vars for Spring Boot

docker compose up -d postgres
mvn spring-boot:run
```

`.env.example` already points `POSTGRES_HOST`/`POSTGRES_PORT` at `localhost:5433`, matching the
compose file's host port mapping. Flyway migrations run automatically on first
`mvn spring-boot:run` — no separate migration command needed.

### Build and run the JAR directly

```bash
mvn clean package
set -a && source .env && set +a
java -jar target/layered-architecture.jar
```

### Troubleshooting Docker

| Problem | Fix |
|---------|-----|
| Port 5433/3011 already in use | Stop conflicting services or change host ports in `docker-compose.yml` |
| `api` exits on Flyway error | `docker compose logs api`; ensure Postgres is healthy |
| Docker daemon not running | Start Docker Desktop |
| Maven build fails locally | Ensure Java 21: `java -version` |

---

## Hosting platforms (free → paid)

Platforms are listed in approximate cost order. Pick based on traffic, team size, and how much ops
work you want to do. This project is deliberately small (one API, one database) so most of these
tiers are more headroom than you'll need for a demo — but the mapping is the same shape as every
other project in this series.

### Tier 1 — Free / learning & demos

| Platform | What to host | Limits / notes |
|----------|--------------|----------------|
| **[Fly.io](https://fly.io/)** | API container | Free allowance; good for one small VM + volume |
| **[Render](https://render.com/)** | API (free web service) | Spins down after inactivity; cold starts |
| **[Railway](https://railway.app/)** | API + add-on Postgres | Trial credits; easy Docker deploy |
| **[Neon](https://neon.tech/)** | PostgreSQL | Free serverless Postgres; replace `postgres` service |
| **[Oracle Cloud Always Free](https://www.oracle.com/cloud/free/)** | Full stack on one ARM VM | Run `docker compose` on a free Ampere instance |
| **[Google Cloud free tier](https://cloud.google.com/free)** | e2-micro VM | Run entire compose file on one VM |

**Recommended free combo:** Neon (Postgres) + Fly.io or Render (API). Set connection env vars
instead of a self-hosted `postgres` container.

### Tier 2 — Hobby / small production ($5–50/mo)

| Platform | Best for | Typical cost |
|----------|----------|--------------|
| **[DigitalOcean Droplet](https://www.digitalocean.com/products/droplet)** | Single VM running full `docker compose` | ~$6–12/mo |
| **[Hetzner Cloud](https://www.hetzner.com/cloud)** | Same as DO; strong price/performance in EU | ~€4–10/mo |
| **[Railway](https://railway.app/)** | Managed Postgres + API from GitHub | ~$5–20/mo |
| **[Render](https://render.com/)** | Managed Postgres + web service (no cold start on paid) | ~$7–25/mo |
| **[Fly.io](https://fly.io/)** | API + attached volumes | ~$5–15/mo |

**Recommended hobby stack:** One $6–12/mo VPS with Docker Compose (simplest ops) **or**
Railway/Render with a managed database (less ops, slightly higher cost).

### Tier 3 — Production / growth ($50–500+/mo)

| Platform | Components | Notes |
|----------|------------|-------|
| **[AWS](https://aws.amazon.com/)** | ECS Fargate or EKS (API), RDS PostgreSQL | Full control; use Secrets Manager for DB credentials |
| **[Google Cloud](https://cloud.google.com/)** | Cloud Run or GKE, Cloud SQL | Good autoscaling on Cloud Run |
| **[Azure](https://azure.microsoft.com/)** | Container Apps or AKS, Azure Database for PostgreSQL | Enterprise integrations |
| **[DigitalOcean](https://www.digitalocean.com/)** | App Platform or DOKS, Managed Postgres | Simpler than big three clouds |
| **[Supabase](https://supabase.com/)** | Postgres | Pro from ~$25/mo |

**Recommended production stack (AWS example):**

```
Internet → ALB → ECS Fargate (API, 2+ tasks)
              → RDS PostgreSQL (Multi-AZ)
Secrets: AWS Secrets Manager (DB password)
Logs: CloudWatch
```

### Tier 4 — Scale / enterprise ($500+/mo)

| Need | Options |
|------|---------|
| Multi-region API | Cloudflare + regional ECS/GKE clusters |
| Read replicas | RDS read replicas if `GET /books` traffic grows heavy |
| CDN for static assets | Cloudflare, Fastly, CloudFront (when a frontend is added) |
| Observability | Datadog, New Relic, Grafana Cloud |

---

## Per-component production mapping

| Local service | Managed alternative (free → paid) | Connection env var |
|---------------|-----------------------------------|--------------------|
| `postgres` | Neon → Supabase → RDS / Cloud SQL | `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB` |
| `api` | Fly.io → Render → ECS / Cloud Run / K8s | `PORT` |

---

## Additional tools for a functional production system

These are not in the repo but are expected in real deployments:

### CI/CD

| Tool | Purpose |
|------|---------|
| [GitHub Actions](https://github.com/features/actions) | `mvn test` → `mvn clean package` → build Docker image → deploy |
| [GitLab CI](https://about.gitlab.com/stages-devops-lifecycle/continuous-integration/) | Same |
| [Argo CD](https://argo-cd.readthedocs.io/) | GitOps deploys to Kubernetes |

Example pipeline steps: `mvn test` → `mvn clean package -DskipTests` → `docker build` → push to
registry → deploy. Flyway migrations run on container startup (or as a one-off init container
before traffic).

### Secrets & config

| Tool | Purpose |
|------|---------|
| [Doppler](https://www.doppler.com/) | Centralized secrets |
| [AWS Secrets Manager](https://aws.amazon.com/secrets-manager/) | Cloud-native secrets |
| [HashiCorp Vault](https://www.vaultproject.io/) | Enterprise secret store |
| [Spring Cloud Config](https://spring.io/projects/spring-cloud-config) | Centralized config for Spring apps |

Never commit production database passwords.

### DNS, TLS, edge

| Tool | Purpose |
|------|---------|
| [Cloudflare](https://www.cloudflare.com/) | DNS, DDoS protection, free TLS |
| [Let's Encrypt](https://letsencrypt.org/) | TLS certificates (often via Caddy or cert-manager) |

### Monitoring & alerting

| Tool | Purpose |
|------|---------|
| [Sentry](https://sentry.io/) | Error tracking (Java SDK available) |
| [Micrometer](https://micrometer.io/) + [Prometheus](https://prometheus.io/) + [Grafana](https://grafana.com/) | Spring Boot metrics and dashboards |
| [UptimeRobot](https://uptimerobot.com/) | Free uptime checks |
| [PagerDuty](https://www.pagerduty.com/) / [Opsgenie](https://www.atlassian.com/software/opsgenie) | On-call alerts |

### Logging

| Tool | Purpose |
|------|---------|
| [Better Stack](https://betterstack.com/) | Log aggregation (free tier) |
| [AWS CloudWatch Logs](https://aws.amazon.com/cloudwatch/) | If on AWS |
| [Loki](https://grafana.com/oss/loki/) | Self-hosted log store |

### Database operations

| Tool | Purpose |
|------|---------|
| [pg_dump](https://www.postgresql.org/docs/current/app-pgdump.html) | Backups |
| [Flyway](https://flywaydb.org/) | Migrations (runs on startup; versioned SQL in `src/main/resources/db/migration/`) |
| [pgBouncer](https://www.pgbouncer.org/) | Connection pooling at scale |

---

## Environment variables (production checklist)

| Variable | Required | Notes |
|----------|----------|-------|
| `PORT` | Yes | Usually `3011` or platform-assigned |
| `POSTGRES_*` | Yes | Use managed DB host in cloud |

---

## Deployment workflow (summary)

1. **Build:** `mvn clean package` → `docker build -t riverside-library-api .`
2. **Push:** Tag and push to ECR, GCR, Docker Hub, or GHCR
3. **Migrate:** Flyway runs on startup (`java -jar target/layered-architecture.jar`); for
   zero-downtime deploys, run migrations as a separate job first
4. **Deploy:** Start API with all env vars pointing at a managed Postgres instance
5. **Verify:** `GET /books`, run the borrow/return walkthrough once, confirm `409`s on the
   business-rule violations
6. **Monitor:** error rate, response latency, DB connection pool saturation

---

## Cost estimate (rough monthly)

| Scenario | Components | Est. cost |
|----------|------------|-----------|
| Local dev | Docker on laptop | $0 |
| Free cloud demo | Neon + Fly.io free | $0 |
| Hobby VPS | 1× 1GB Droplet, full compose | ~$6 |
| Small production | Managed DB + 2 API instances | ~$40–100 |
| Growth | RDS Multi-AZ + ECS autoscaling | ~$150–400+ |

---

## Related docs

- [README.md](./README.md) — architecture, curl walkthrough
- [../README.md](../README.md) — full system design series index (Java edition)
- [../../system-design/11-layered-architecture/HOSTING.md](../../system-design/11-layered-architecture/HOSTING.md) — TypeScript/NestJS edition (same topology)
