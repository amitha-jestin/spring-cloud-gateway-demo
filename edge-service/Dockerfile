# =========================
# Builder stage
# =========================
FROM eclipse-temurin:21-jdk AS builder

# Install Maven
RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /build

# Copy the entire project
COPY pom.xml .
COPY booking-service/pom.xml booking-service/
COPY edge-service/pom.xml edge-service/

COPY booking-service/src booking-service/src
COPY edge-service/src edge-service/src

# Build edge-service and its dependencies
RUN mvn clean package -pl edge-service -am -DskipTests

# =========================
# OTel stage
# =========================
FROM curlimages/curl:latest AS otel-agent

ARG OTEL_AGENT_VERSION=2.14.0

RUN curl -fSL -o /tmp/opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar"

# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

# Create non-root user
RUN groupadd -r gateway && \
    useradd -r -g gateway gateway

# OpenTelemetry Agent — copied from dedicated download stage
RUN mkdir -p /otel
COPY --from=otel-agent /tmp/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

RUN chown -R gateway:gateway /app /otel

USER gateway

COPY --from=builder /build/edge-service/target/edge-service-*.jar app.jar

ENTRYPOINT ["java", \
  "-javaagent:/otel/opentelemetry-javaagent.jar", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-Dotel.service.name=edge-service", \
  "-jar", "app.jar"]