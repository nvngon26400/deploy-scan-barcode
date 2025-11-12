# syntax=docker/dockerfile:1

# ---- Build stage: use Gradle with JDK 17 ----
FROM gradle:8.7.0-jdk17 AS builder
WORKDIR /workspace

# Copy entire repo (includes demo module)
COPY . .

# Build Spring Boot jar (skip tests)
RUN chmod +x demo/gradlew && \
    cd demo && ./gradlew clean build -x test && \
    mkdir -p /artifacts && \
    JAR=$(ls build/libs/*-SNAPSHOT.jar 2>/dev/null | head -n 1 || ls build/libs/*.jar 2>/dev/null | head -n 1) && \
    cp "$JAR" /artifacts/app.jar

# ---- Runtime stage: lightweight JRE 17 ----
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy built jar from builder stage
COPY --from=builder /artifacts/app.jar /app/app.jar

# Expose default port (Spring reads PORT env from Railway)
EXPOSE 8080

# Start the application
CMD ["java", "-jar", "/app/app.jar"]