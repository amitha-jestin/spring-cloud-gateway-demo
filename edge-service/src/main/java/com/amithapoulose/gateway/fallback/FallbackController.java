package com.amithapoulose.gateway.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;

/**
 * FallbackController — Structured fallback responses when circuit breaker is OPEN.
 *
 * Invoked when: circuit breaker opens, upstream timeout, service unreachable.
 * Returns actionable responses — never a raw 502.
 */
@Slf4j
@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/booking")
    public Mono<ResponseEntity<Map<String, Object>>> bookingFallback() {
        log.warn("Circuit breaker OPEN — fallback for booking-service");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 503,
                "error", "Service Temporarily Unavailable",
                "message", "Booking service is currently unavailable. Please retry in 30 seconds.",
                "retryAfterSeconds", 30,
                "supportReference", "CB-BOOKING-OPEN"
        )));
    }

    @RequestMapping("/default")
    public Mono<ResponseEntity<Map<String, Object>>> defaultFallback() {
        log.warn("Circuit breaker OPEN — default fallback triggered");
        return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "timestamp", Instant.now().toString(),
                "status", 503,
                "error", "Service Temporarily Unavailable",
                "message", "The requested service is temporarily unavailable. Please retry in 30 seconds.",
                "retryAfterSeconds", 30
        )));
    }
}
