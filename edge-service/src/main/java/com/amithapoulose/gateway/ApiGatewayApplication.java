package com.amithapoulose.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Cloud Gateway Reference Implementation.
 *
 * Cross-cutting concerns handled at the gateway layer:
 *
 * 1. Security     — OAuth2/OIDC JWT validation via any OIDC provider (Keycloak, Cognito, Entra ID)
 *                   Offline JWKS validation — no call to identity provider per request
 *                   Redis rate limiting per authenticated user identity
 *
 * 2. Resilience   — Resilience4J circuit breaker per route
 *                   Retry on GET routes only (never on writes — duplicate risk)
 *                   Structured fallback responses when circuit is open
 *                   Hard timeouts per route
 *
 * 3. Observability — Micrometer metrics → Prometheus → Grafana
 *                    OpenTelemetry traces → Tempo → Grafana
 *                    Structured JSON logs → Loki → Grafana
 *                    Audit log stream — auth failures, rate limit hits, CB events
 *
 * Filter pipeline (per request, in order):
 *   TraceIdFilter → AuthHeaderPropagation → RateLimiter → CircuitBreaker → AuditLog → Route
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
