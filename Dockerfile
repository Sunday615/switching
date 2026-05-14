# ── Stage 1: Dependency cache ─────────────────────────────────────────────────
# Copy pom.xml first so Maven downloads dependencies before copying source.
# This layer is cached and reused on subsequent builds when only src/ changes.
FROM maven:3.9.9-eclipse-temurin-21 AS deps
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw -q dependency:go-offline

# ── Stage 2: Build JAR ────────────────────────────────────────────────────────
# Tests are intentionally skipped here because integration tests use
# Testcontainers which requires a Docker daemon — not available inside
# a Docker build context. Tests must be run and pass in CI before this
# image is built (see .github/workflows/ci.yml job order).
FROM deps AS build
COPY src src
RUN ./mvnw -q clean package -DskipTests

# ── Stage 3: Runtime ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Run as non-root user (RISK-DEP-002)
RUN addgroup --system switching && adduser --system --ingroup switching switching
USER switching

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]