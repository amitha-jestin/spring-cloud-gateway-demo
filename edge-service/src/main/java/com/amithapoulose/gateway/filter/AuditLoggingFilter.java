package com.amithapoulose.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * AuditLoggingFilter — Structured audit log for every request.
 *
 * Pipeline position: LAST before route — runs after all security filters.
 *
 * Why position matters:
 * If placed before the rate limiter, rate-limited requests won't appear in audit logs.
 * If placed after, every rejected request is still audited.
 * In regulated environments (PCI-DSS, SOC2), rejected requests MUST be audited.
 *
 * Every log entry contains enough context to reconstruct the full request journey:
 * traceId, userId, method, path, status, duration, clientIp
 *
 * The audit log is separate from application logs for compliance reasons.
 * In production, route AUDIT log entries to a separate, immutable stream.
 *
 * Loki LogQL to query audit logs:
 *   {app="api-gateway"} |= "AUDIT" | json
 *
 * Loki LogQL to find all auth failures:
 *   {app="api-gateway"} |= "AUDIT_SECURITY"
 *
 * Loki LogQL to find slow requests:
 *   {app="api-gateway"} |= "AUDIT_SLOW"
 */
@Slf4j
@Component
public class AuditLoggingFilter implements GlobalFilter, Ordered {

    private static final long SLOW_REQUEST_THRESHOLD_MS = 3000;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("AUDIT_LOGGING_FILTER: Processing request for path: {}", exchange.getRequest().getPath().value());
        long startTime = Instant.now().toEpochMilli();
        String traceId = exchange.getAttributes()
                .getOrDefault(TraceIdFilter.TRACE_ID_HEADER, "unknown").toString();

        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String clientIp = getClientIp(exchange);
        String method = exchange.getRequest().getMethod().name();
        String path = exchange.getRequest().getPath().value();

        return chain.filter(exchange).doFinally(signal -> {
            long durationMs = Instant.now().toEpochMilli() - startTime;
            HttpStatus status = (HttpStatus) exchange.getResponse().getStatusCode();
            int statusCode = status != null ? status.value() : 0;

            // Standard audit log — every request
            log.info("AUDIT traceId={} userId={} method={} path={} status={} durationMs={} clientIp={}",
                    traceId, nullSafe(userId), method, path, statusCode, durationMs, clientIp);

            // Security audit — auth and rate limit failures
            if (statusCode == 401 || statusCode == 403) {
                log.warn("AUDIT_SECURITY traceId={} userId={} method={} path={} status={} clientIp={}",
                        traceId, nullSafe(userId), method, path, statusCode, clientIp);
            }

            if (statusCode == 429) {
                log.warn("AUDIT_RATE_LIMIT traceId={} userId={} path={} clientIp={}",
                        traceId, nullSafe(userId), path, clientIp);
            }

            // Performance audit — slow requests
            if (durationMs > SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("AUDIT_SLOW traceId={} userId={} path={} durationMs={}",
                        traceId, nullSafe(userId), path, durationMs);
            }

            // Circuit breaker opened — upstream unavailable
            if (statusCode == 503) {
                log.warn("AUDIT_CB_OPEN traceId={} userId={} path={} durationMs={}",
                        traceId, nullSafe(userId), path, durationMs);
            }
        });
    }

    private String getClientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }

    private String nullSafe(String v) {
        return v != null ? v : "anonymous";
    }

    @Override
    public int getOrder() {
        return -1; // Run after all other filters
    }
}