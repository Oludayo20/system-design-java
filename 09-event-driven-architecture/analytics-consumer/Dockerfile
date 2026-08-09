FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN addgroup --system analytics && adduser --system --ingroup analytics analytics

COPY --from=build /app/target/analytics-consumer.jar ./app.jar

USER analytics

EXPOSE 4103

CMD ["java", "-jar", "app.jar"]
