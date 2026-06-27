FROM eclipse-temurin:21-jdk AS builder

RUN apt-get update && \
    apt-get install -y maven && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /build

COPY pom.xml .
COPY booking-service/pom.xml booking-service/
COPY edge-service/pom.xml edge-service/

COPY booking-service/src booking-service/src
COPY edge-service/src edge-service/src

RUN mvn clean package -pl booking-service -am -DskipTests

FROM curlimages/curl:latest AS otel-agent

ARG OTEL_AGENT_VERSION=2.14.0

RUN curl -fSL -o /tmp/opentelemetry-javaagent.jar \
    "https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v${OTEL_AGENT_VERSION}/opentelemetry-javaagent.jar"


FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app
RUN groupadd -r booking && useradd -r -g booking booking

RUN mkdir -p /otel
COPY --from=otel-agent /tmp/opentelemetry-javaagent.jar /otel/opentelemetry-javaagent.jar

RUN chown -R booking:booking /app /otel

USER booking

COPY --from=builder /build/booking-service/target/booking-service-*.jar app.jar
ENTRYPOINT ["java", \
  "-javaagent:/otel/opentelemetry-javaagent.jar", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
