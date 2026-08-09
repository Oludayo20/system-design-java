FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN addgroup --system notification && adduser --system --ingroup notification notification

COPY --from=build /app/target/notification-consumer.jar ./app.jar

USER notification

EXPOSE 4102

CMD ["java", "-jar", "app.jar"]
