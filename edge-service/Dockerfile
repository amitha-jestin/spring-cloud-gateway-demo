FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/
COPY api-gateway/src api-gateway/src
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw clean package -pl api-gateway -am -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S gateway && adduser -S gateway -G gateway
USER gateway
COPY --from=builder /build/api-gateway/target/api-gateway-*.jar app.jar
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
