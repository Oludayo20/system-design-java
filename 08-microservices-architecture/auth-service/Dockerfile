FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app

COPY --from=build /app/target/auth-service.jar ./app.jar

USER app

EXPOSE 4001

CMD ["java", "-jar", "app.jar"]
