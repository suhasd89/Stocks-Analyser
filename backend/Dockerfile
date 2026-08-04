# Build stage
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -q -DskipTests package

# Runtime stage
FROM eclipse-temurin:25-jre

WORKDIR /app

# Copy built JAR from build stage
COPY --from=build /workspace/target/stock-signal-service-0.0.1-SNAPSHOT.jar app.jar

# Create data directory for SQLite and logs
RUN mkdir -p /app/data

EXPOSE 8080

# Enable container shutdown with SIGTERM
ENV JAVA_TOOL_OPTIONS="-XX:+UseG1GC -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
