# ==================================================
# Multi-stage Dockerfile for Spring Cucumber Tests
# ==================================================

# ==================================================
# Stage 1: Build Stage
# ==================================================
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy dependency definitions
COPY build.gradle settings.gradle ./
COPY gradle ./gradle

# Download dependencies (cached layer)
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY src ./src

# Build the application (skip tests in build stage)
RUN gradle clean build -x test --no-daemon

# ==================================================
# Stage 2: Runtime Stage
# ==================================================
FROM openjdk:17-slim

# Install required packages
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    unzip \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy Gradle wrapper and configs
COPY --from=builder /app/gradle ./gradle
COPY --from=builder /app/gradlew ./
COPY --from=builder /app/build.gradle ./
COPY --from=builder /app/settings.gradle ./

# Copy built dependencies
COPY --from=builder /root/.gradle /root/.gradle

# Copy source and resources
COPY src ./src

# Copy test configurations
COPY testng.xml ./
COPY testng-cucumber.xml ./
COPY testng-smoke.xml ./

# Create directories for reports and logs
RUN mkdir -p /app/build /app/ExtentReports /app/logs /app/downloads

# Set permissions
RUN chmod +x gradlew

# Environment variables with defaults
ENV BROWSER=chrome
ENV HEADLESS=false
ENV REMOTE_EXECUTION=false
ENV REMOTE_URL=http://localhost:4444/wd/hub
ENV SPRING_PROFILES_ACTIVE=dev
ENV JAVA_OPTS="-Xmx2g -Xms512m"

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Default command - run tests
CMD ["sh", "-c", "./gradlew clean test -Dremote.execution=${REMOTE_EXECUTION} -Dremote.url=${REMOTE_URL} -Dbrowser=${BROWSER} -Dheadless=${HEADLESS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} --no-daemon"]

# ==================================================
# Build Instructions:
# ==================================================
# Build image:
#   docker build -t spring-cucumber-tests:latest .
#
# Run tests locally:
#   docker run --rm spring-cucumber-tests:latest
#
# Run tests with Grid:
#   docker run --rm --network selenium-grid-network \
#     -e REMOTE_EXECUTION=true \
#     -e REMOTE_URL=http://selenium-hub:4444/wd/hub \
#     spring-cucumber-tests:latest
#
# Run with volume mounts for reports:
#   docker run --rm \
#     -v $(pwd)/build:/app/build \
#     -v $(pwd)/ExtentReports:/app/ExtentReports \
#     spring-cucumber-tests:latest
# ==================================================
