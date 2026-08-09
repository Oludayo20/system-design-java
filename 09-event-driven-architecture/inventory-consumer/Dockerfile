FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN addgroup --system inventory && adduser --system --ingroup inventory inventory

COPY --from=build /app/target/inventory-consumer.jar ./app.jar

USER inventory

EXPOSE 4101

CMD ["java", "-jar", "app.jar"]
