FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN addgroup --system loyalty && adduser --system --ingroup loyalty loyalty

COPY --from=build /app/target/loyalty-consumer.jar ./app.jar

USER loyalty

EXPOSE 4104

CMD ["java", "-jar", "app.jar"]
