# ════════════════════════════════════════════════════════════════
# STAGE 1 — BUILD
# Uses the full Maven + JDK image to compile and package the app.
# This stage is heavy (~700 MB) but it's DISCARDED after building.
# ════════════════════════════════════════════════════════════════
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# ── Dependency caching trick ──────────────────────────────────────
# Copy ONLY pom.xml first and download all dependencies.
# Docker caches this layer. On the next build, if pom.xml hasn't
# changed, Maven won't re-download the internet. Saves ~2-3 min.
COPY pom.xml .
RUN mvn dependency:go-offline -B

# ── Now copy the source code and build ───────────────────────────
# -DskipTests → tests are run by Jenkins separately (with reports)
# -B           → batch mode (no interactive prompts in CI)
COPY src ./src
RUN mvn package -DskipTests -B

# ════════════════════════════════════════════════════════════════
# STAGE 2 — RUN
# Uses a minimal JRE-only Alpine image (~180 MB).
# No Maven, no source code, no build tools — smaller attack surface.
# ════════════════════════════════════════════════════════════════
FROM eclipse-temurin:17-jre-alpine AS runner

# ── Security: never run as root inside a container ───────────────
# Creates a system group and user called "appuser"
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# ── Copy only the JAR from the builder stage ─────────────────────
COPY --from=builder /app/target/*.jar app.jar

# ── Give ownership to the non-root user ──────────────────────────
RUN chown appuser:appgroup app.jar

# Switch to non-root user
USER appuser

# ── Port the app listens on (matches application.yml) ────────────
EXPOSE 8080

# ── Health check ─────────────────────────────────────────────────
# Docker (and docker-compose) will poll this every 30s.
# Kubernetes uses its own liveness probe (Step 6), but this is
# good practice for local development.
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# ── JVM tuning flags ─────────────────────────────────────────────
# -XX:+UseContainerSupport       → JVM respects container memory limits (not host RAM)
# -XX:MaxRAMPercentage=75.0      → JVM uses max 75% of container memory
# -Djava.security.egd            → faster startup (entropy source for Tomcat)
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]