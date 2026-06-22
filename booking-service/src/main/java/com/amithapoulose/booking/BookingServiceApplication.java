package com.amithapoulose.booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Booking Service — Event ticket booking microservice.
 *
 * Security model: TRUST THE GATEWAY.
 * This service sits behind the API Gateway inside the cluster.
 * It is never directly reachable from the internet.
 *
 * Authentication: handled by the gateway (JWT validated there).
 * Authorization:  enforced in controllers via X-User-Roles header.
 *
 * The gateway propagates verified identity as headers:
 *   X-User-Id    → user's unique identifier
 *   X-User-Email → user's email
 *   X-User-Roles → comma-separated roles
 *   X-Trace-Id   → distributed trace ID
 */
@SpringBootApplication
public class BookingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }
}
