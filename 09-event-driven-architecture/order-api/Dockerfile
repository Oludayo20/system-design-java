FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN addgroup --system orderapi && adduser --system --ingroup orderapi orderapi

COPY --from=build /app/target/order-api.jar ./app.jar

USER orderapi

EXPOSE 3009

CMD ["java", "-jar", "app.jar"]
