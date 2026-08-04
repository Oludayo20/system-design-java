# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

# --- Runtime stage ---
# This single image is used for BOTH api-1 and api-2 in docker-compose.yml.
# The app is stateless (all state lives in Postgres/Redis/RabbitMQ, config
# comes entirely from env vars) - that statelessness is what makes running
# two identical replicas behind Nginx a valid horizontal-scaling move rather
# than two containers that happen to disagree with each other.
FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S oja && adduser -S oja -G oja

COPY --from=build /app/target/ecom-marketplace-capstone.jar ./app.jar

USER oja

EXPOSE 3000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
