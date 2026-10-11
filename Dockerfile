
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN useradd --system --uid 1001 appuser

COPY --from=build /workspace/target/campusflow.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-XX:+UseSerialGC", "-jar", "/app/app.jar"]
