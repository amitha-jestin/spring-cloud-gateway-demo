package com.amithapoulose.gateway.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JConfigBuilder;
import org.springframework.cloud.client.circuitbreaker.Customizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4jConfig — Circuit breaker configuration for all routes.
 *
 * Why circuit breaking at the gateway?
 * A circuit breaker inside one service protects only that service.
 * At the gateway, one circuit breaker protects every service behind it simultaneously.
 * When a service degrades, the gateway stops hammering it — giving it time to recover.
 *
 * Circuit breaker states:
 * CLOSED    → Normal operation. Requests pass through.
 * OPEN      → Too many failures. Requests fail fast → fallback response returned.
 * HALF_OPEN → Testing recovery. Limited probe requests let through.
 *
 * Key configuration decisions:
 *
 * slowCallDurationThreshold: 3s
 *   A service returning 200 OK after 8 seconds is functionally a failure.
 *   Thread pools drain. Users wait. HTTP status alone doesn't capture this.
 *   Slow calls count as failures — circuit opens on both errors AND slowness.
 *
 * waitDurationInOpenState: 30s
 *   How long the circuit stays open before testing recovery.
 *   Too short: hammers a recovering service. Too long: unnecessary downtime.
 *
 * permittedNumberOfCallsInHalfOpenState: 3
 *   How many probe requests to send before deciding if service has recovered.
 *   If 2/3 succeed, circuit closes. If they fail, reopens for another 30s.
 */
@Configuration
public class Resilience4jConfig {

    @Bean
    public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCircuitBreakerCustomizer() {
        return factory -> factory.configureDefault(id ->
                new Resilience4JConfigBuilder(id)
                        .circuitBreakerConfig(defaultCircuitBreakerConfig())
                        .timeLimiterConfig(defaultTimeLimiterConfig())
                        .build()
        );
    }

    /**
     * Default circuit breaker — applied to all routes unless overridden.
     * Per-route overrides can be added via additional @Bean customizers.
     */
    private CircuitBreakerConfig defaultCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                // Count-based sliding window — evaluate last 10 calls
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)

                // Open circuit if 50% of calls fail
                .failureRateThreshold(50.0f)

                // Slow calls (>3s) also count as failures — not just HTTP errors
                .slowCallDurationThreshold(Duration.ofSeconds(3))
                .slowCallRateThreshold(50.0f)

                // Minimum calls before evaluating — prevents opening on first single failure
                .minimumNumberOfCalls(5)

                // Stay OPEN for 30 seconds before moving to HALF_OPEN
                .waitDurationInOpenState(Duration.ofSeconds(30))

                // Allow 3 calls in HALF_OPEN to test recovery
                .permittedNumberOfCallsInHalfOpenState(3)

                // Automatically transition OPEN → HALF_OPEN after wait duration
                .automaticTransitionFromOpenToHalfOpenEnabled(true)

                .recordExceptions(
                        java.io.IOException.class,
                        java.util.concurrent.TimeoutException.class
                )
                .build();
    }

    /**
     * Hard timeout per request.
     * If upstream doesn't respond within 5s, gateway cancels and returns fallback.
     */
    private TimeLimiterConfig defaultTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))
                .cancelRunningFuture(true)
                .build();
    }
}
