# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
# Warm the local repo cache from pom.xml alone so source-only edits don't invalidate this layer.
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage ---
# One image, two entrypoints: ApiApplication (producer) and WorkerApplication (consumer), both
# packaged into the same executable jar (spring-boot-maven-plugin, layout=PropertiesLauncher).
# Which one runs is chosen by the LOADER_MAIN env var set in docker-compose.yml's `command`/
# `environment`, so `docker compose up --scale worker=20` spins up 20 containers from this same
# image, all launching WorkerApplication, independent of the single `api` container — the same
# shape as the original's `node dist/main.js` vs `node dist/worker.main.js`.
FROM eclipse-temurin:21-jre-alpine AS runtime

ENV JAVA_TOOL_OPTIONS=""
WORKDIR /app

RUN addgroup -S asyncqueue && adduser -S asyncqueue -G asyncqueue

COPY --from=build /app/target/async-queue-processing.jar app.jar

USER asyncqueue

EXPOSE 3000

# Default: API. Override by setting LOADER_MAIN=com.systemdesign.asyncqueue.worker.WorkerApplication
# (see docker-compose.yml's `worker` service).
ENTRYPOINT ["java", "-jar", "app.jar"]
