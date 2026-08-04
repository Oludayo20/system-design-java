# --- Build stage ---
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN addgroup --system eshop && adduser --system --ingroup eshop eshop

COPY --from=build /app/target/modular-monolith.jar ./app.jar

USER eshop

EXPOSE 3000

CMD ["java", "-jar", "app.jar"]
