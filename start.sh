#!/bin/bash
set -eu

# Resolve Java binary reliably in Nixpacks
JAVA_BIN="/nix/var/nix/profiles/default/bin/java"
if [ ! -x "$JAVA_BIN" ]; then
  JAVA_BIN=$(command -v java || true)
fi
if [ -z "${JAVA_BIN:-}" ]; then
  echo "Java binary not found. Listing /nix/var/nix/profiles/default/bin:" >&2
  ls -la /nix/var/nix/profiles/default/bin || true
  exit 1
fi

# Verify Java is available
"$JAVA_BIN" -version || true

# Move into the Spring Boot app directory
cd demo

# Ensure Gradle wrapper is executable
chmod +x ./gradlew || true

# Build the application (skip tests for faster deploy)
./gradlew clean build -x test

# Find the generated Spring Boot jar
JAR_FILE=$(ls build/libs/*-SNAPSHOT.jar | head -n 1)

# Fallback if pattern did not match
if [ -z "${JAR_FILE:-}" ]; then
  JAR_FILE=$(ls build/libs/*.jar | head -n 1)
fi

# Run the app binding to Railway's PORT if provided
exec "$JAVA_BIN" -jar "$JAR_FILE"