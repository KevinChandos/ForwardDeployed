# ============================================================
# Dockerfile
# Purpose: Multi-stage build for the application container.
# Stage 1 (builder): Compiles the artifact using Maven.
# Stage 2 (runtime): Runs the compiled JAR on a minimal JRE.
# Side effect: Uses a non-root user (appuser) at runtime to
#              reduce the attack surface. The JAR is expected
#              at target/*.jar after `mvn package`.
# Build args:
#   BUILD_DATE  — ISO-8601 timestamp injected as OCI label
#   GIT_COMMIT  — full Git SHA injected as OCI label
# ============================================================

# ---- Stage 1: Build ----
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /workspace

# Copy dependency descriptors first to leverage layer caching
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode dependency:go-offline -q

COPY src ./src

RUN --mount=type=cache,target=/root/.m2 \
    mvn --batch-mode clean package -DskipTests

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine AS runtime

ARG BUILD_DATE
ARG GIT_COMMIT

# OCI standard image labels for traceability
LABEL org.opencontainers.image.created="${BUILD_DATE}"
LABEL org.opencontainers.image.revision="${GIT_COMMIT}"
LABEL org.opencontainers.image.title="my-app"
LABEL org.opencontainers.image.vendor="My Organisation"

# Create a non-root user to run the application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# Copy only the executable JAR from the builder stage
COPY --from=builder /workspace/target/*.jar app.jar

RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# Use exec form to forward OS signals correctly to the JVM
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
