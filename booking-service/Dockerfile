FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build
COPY pom.xml .
COPY booking-service/pom.xml booking-service/
COPY booking-service/src booking-service/src
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw && ./mvnw clean package -pl booking-service -am -DskipTests

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S booking && adduser -S booking -G booking
USER booking
COPY --from=builder /build/booking-service/target/booking-service-*.jar app.jar
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
