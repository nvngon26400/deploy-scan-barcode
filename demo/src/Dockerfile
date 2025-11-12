# syntax=docker/dockerfile:1

# ---- Build stage ----
FROM eclipse-temurin:17-jdk AS builder
WORKDIR /workspace

# Copy Gradle wrapper and build files for better layer caching
COPY gradlew gradlew.bat settings.gradle build.gradle ./
COPY gradle ./gradle

# Ensure wrapper is executable in Linux environment
RUN chmod +x gradlew

# Copy source code
COPY src ./src

# Pre-flight to initialize Gradle wrapper (downloads Gradle)
RUN ./gradlew --version

# Build application JAR (skip tests)
RUN ./gradlew clean bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:17-jre AS runner
WORKDIR /app

# Volumes for persistent data and uploads
VOLUME ["/app/data", "/app/uploads"]

# Copy the built jar from the builder stage
COPY --from=builder /workspace/build/libs/*.jar /app/app.jar

# Default environment variables
ENV SPRING_PROFILES_ACTIVE=default
ENV JAVA_OPTS=""

# Documentative port exposure
EXPOSE 8080

# Run the application. Respect platform-provided PORT if exists.
CMD sh -lc "java $JAVA_OPTS -Dserver.port=${PORT:-8080} -jar /app/app.jar"