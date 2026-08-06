# Hosting & Deployment Guide — CAP Theorem Demo (Java / Spring Boot)

This document explains how to run the CAP theorem demonstration locally (including Docker) and how it fits into production hosting decisions. The app is a **single Spring Boot process** simulating two in-memory database nodes with a partition toggle — no external database required.

For AP vs CP walkthrough, see [README.md](./README.md).

---

## Application components

| Component | Role | Required? | Notes |
|-----------|------|-----------|-------|
| **Spring Boot API** | HTTP server; simulates nodes A and B | Yes | Port `3006` default |
| **In-memory cluster** | Two logical nodes with sync/partition state | Yes | Lost on restart |
| **AP endpoint** | `POST /profile/view` — accepts writes during partition | Yes | Like product view counters |
| **CP endpoint** | `POST /wallet/debit` — rejects writes during partition | Yes | Like wallet balances |
| **Admin endpoints** | Partition toggle, reconcile | Yes | Teaching only — lock down in prod |

**Stack:** Spring Boot 3.3 / Java 21 / Maven / springdoc-openapi at `/docs`.

---

## Prerequisites (your machine)

| Tool | Version | Purpose |
|------|---------|---------|
| Java (Temurin) | 21 | Run API |
| Maven | 3.9+ | Build, test, run |
| Docker Desktop | Latest | Optional containerized run via `Dockerfile` + `docker-compose.yml` |
| curl | Any | Walkthrough |

---

## Run locally with Docker

This project includes a **`Dockerfile`** and **`docker-compose.yml`** for a one-command containerized run.

### Quick start

```bash
cd system-design-java/06-cap-theorem
cp .env.example .env
docker compose up --build
```

The `Dockerfile` multi-stage build:

1. **Build stage:** `maven:3.9-eclipse-temurin-21` → `mvn clean package -DskipTests`
2. **Runtime stage:** `eclipse-temurin:21-jre` → `java -jar app.jar` (from `target/cap-theorem.jar`)

| Endpoint | URL |
|----------|-----|
| Swagger | http://localhost:3006/docs |
| Node state | `GET http://localhost:3006/nodes` |
| Enable partition | `POST http://localhost:3006/admin/partition` |
| Reconcile | `POST http://localhost:3006/admin/reconcile` |

### Full walkthrough

```bash
# Healthy cluster
curl http://localhost:3006/nodes

# Simulate network partition
curl -X POST http://localhost:3006/admin/partition \
  -H 'Content-Type: application/json' \
  -d '{"enabled": true}'

# AP: view increment accepted (nodes may diverge)
curl -X POST http://localhost:3006/profile/view
curl http://localhost:3006/nodes

# CP: wallet debit rejected during partition
curl -X POST http://localhost:3006/wallet/debit \
  -H 'Content-Type: application/json' \
  -d '{"amount": 500}'

# Heal partition
curl -X POST http://localhost:3006/admin/reconcile
curl http://localhost:3006/nodes
```

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
java -jar target/cap-theorem.jar
```

### Build Docker image manually

```bash
mvn clean package -DskipTests
docker build -t cap-theorem-api .
docker run -p 3006:3006 --env-file .env cap-theorem-api
```

### Stop

```bash
docker compose down
```

---

## Hosting platforms (free → paid)

Like `05-resilience`, this is a **stateless teaching API** (state is in-memory and ephemeral). Hosting is trivial; the value is understanding tradeoffs when you deploy real distributed data stores.

### Tier 1 — Free

| Platform | Notes |
|----------|-------|
| **Fly.io** | Deploy `Dockerfile`; state resets on restart |
| **Render** | Free web service |
| **Railway** | One-click deploy |
| **Google Cloud Run** | Container from `Dockerfile` |

### Tier 2 — Hobby ($0–7/mo)

Always-on instance if you need a persistent demo URL without cold starts.

### Tier 3 — Production (where CAP actually matters)

This demo maps to **real infrastructure choices** in the Oja capstone and similar systems:

| Feature | CAP choice | Production hosting implication |
|---------|------------|------------------------------|
| Product view counter | **AP** | Redis INCR, Cassandra, DynamoDB with eventual consistency |
| Like counts | **AP** | Same; brief divergence OK |
| Wallet balance | **CP** | Single-shard Postgres transaction (`04` capstone) |
| Order + inventory | **CP** | Primary DB transaction before event publish |
| Product catalog cache | **AP** | Redis cache-aside; stale reads acceptable |
| Search index | **AP** | Elasticsearch / Algolia; async indexing |

| System | Typical CAP posture | Hosting |
|--------|---------------------|---------|
| **PostgreSQL (single leader)** | CP during partition (one writer) | RDS, Cloud SQL — wallet/order data |
| **Redis** | Configurable; often AP for caches | ElastiCache, Upstash |
| **Cassandra / DynamoDB** | AP | Global view counters, activity feeds |
| **Kafka** | AP (availability + partition tolerance) | MSK, Confluent — event streams |
| **Multi-region Postgres** | Hard CP without sync replication lag | Use per-region shards or CRDTs for AP data only |

### Tier 4 — Enterprise

- **Spanner / CockroachDB** — external consistency with global distribution
- **Conflict-free replicated data types (CRDTs)** for AP counters
- **Saga / 2PC** when CP operations span shards (avoid when possible — see `04` wallet colocation)

---

## Tools needed for production CAP-aware systems

### For this demo only

| Tool | Purpose |
|------|---------|
| Java 21 or Docker | Run the simulator |
| `Dockerfile` + `docker-compose.yml` | Containerized deploy |
| Browser / curl | Exercise endpoints |

### For real distributed apps (Oja-scale)

| Tool | CAP relevance | Purpose |
|------|---------------|---------|
| **PostgreSQL per shard** | CP for wallet | `04` capstone shard routing |
| **Redis** | AP cache | Stale product lists OK |
| **RabbitMQ / Kafka** | AP event delivery | Eventually consistent side effects |
| **Consul / etcd** | CP coordination | Leader election, service discovery |
| **ZooKeeper** | CP | Kafka old-style coordination |

### Observability for consistency

| Tool | Purpose |
|------|---------|
| **Micrometer metrics** | Replication lag, cache staleness age |
| **Audit logs** | Wallet debits must be linearizable |
| **Chaos Mesh / Toxiproxy** | Simulate partitions in staging |
| **Jepsen** | Formal consistency testing (advanced) |

### CI/CD

```bash
mvn test   # ClusterService tests — AP accepts, CP rejects during partition
```

No integration infrastructure required for tests.

Example pipeline with Docker:

```bash
mvn test
mvn clean package -DskipTests
docker build -t cap-theorem-api .
# push + deploy
```

---

## Environment variables

| Variable | Default | Notes |
|----------|---------|-------|
| `PORT` | `3006` | HTTP port |

No secrets required for this demo.

---

## Docker architecture note

The container runs **one process** simulating two nodes. In production, "node A" and "node B" would be **separate machines or database replicas** connected by a network that can actually partition. This demo collapses that into one API for teaching — do not confuse the simulation with a highly available deployment.

To demonstrate real partitions in staging:

1. Deploy two API instances with separate in-memory stores (still not true CAP — use real DBs).
2. Use **Toxiproxy** between app and Postgres to drop packets.
3. Use **iptables** rules on a test VM to split replica connectivity.

---

## Mapping to series projects

| Project | CAP lesson |
|---------|------------|
| `01-modular-monolith` | Single Postgres = CP within one node |
| `02-database-sharding` | Partitions increase partition surface area |
| `04-ecom-marketplace-capstone` | Wallet CP on shards; catalog AP via Redis |
| `05-resilience` | What to do when CP node is unavailable (fail vs degrade) |
| **This project** | Explicit AP vs CP policy per endpoint |

---

## Cost estimate

| Scenario | Est. cost |
|----------|-----------|
| Local / Docker | $0 |
| Free PaaS | $0 |
| Production CAP-aware stack (Redis + sharded PG + MQ) | See `04` hosting guide (~$150–600+/mo) |

---

## Related docs

- [README.md](./README.md) — endpoint walkthrough
- [../LEVELS-6-7.md](../LEVELS-6-7.md) — Level 7 CAP concepts
- [../04-ecom-marketplace-capstone/HOSTING.md](../04-ecom-marketplace-capstone/HOSTING.md) — wallet CP in practice
- [../02-database-sharding/HOSTING.md](../02-database-sharding/HOSTING.md) — partition tolerance at scale
- [../../system-design/06-cap-theorem/HOSTING.md](../../system-design/06-cap-theorem/HOSTING.md) — TypeScript/NestJS edition
