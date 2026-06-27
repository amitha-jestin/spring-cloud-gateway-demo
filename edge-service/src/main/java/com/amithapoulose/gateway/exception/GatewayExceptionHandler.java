package com.amithapoulose.gateway.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * GatewayExceptionHandler — Global error handler for the gateway.
 *
 * Ensures all errors return consistent JSON regardless of error type.
 * Runs before DefaultErrorWebExceptionHandler (@Order(-1)).
 *
 * Handles: 401 (missing/invalid JWT), 403 (insufficient role),
 * 429 (rate limit), 503 (circuit open), 504 (timeout), 500 (unexpected)
 */
@Slf4j
@Component
@Order(-1)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        log.info("Handling gateway exception for path");
        HttpStatus status = resolveStatus(ex);
        String traceId = exchange.getAttributes()
                .getOrDefault("X-Trace-Id", "unknown").toString();

        log.error("Gateway error: status={} traceId={} error={}",
                status.value(), traceId, ex.getMessage());

        String body = buildJson(status,
                resolveMessage(status),
                exchange.getRequest().getPath().value(),
                traceId);

        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private HttpStatus resolveStatus(Throwable ex) {
        log.info("Resolving HTTP status for exception");
        if (ex instanceof ResponseStatusException rse) {
            HttpStatus resolved = HttpStatus.resolve(rse.getStatusCode().value());
            return resolved != null ? resolved : HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (ex instanceof org.springframework.security.access.AccessDeniedException)
            return HttpStatus.FORBIDDEN;
        if (ex instanceof org.springframework.security.core.AuthenticationException)
            return HttpStatus.UNAUTHORIZED;
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String resolveMessage(HttpStatus status) {
        return switch (status) {
            case UNAUTHORIZED      -> "Authentication required. Provide a valid Bearer token.";
            case FORBIDDEN         -> "Access denied. Insufficient permissions for this resource.";
            case TOO_MANY_REQUESTS -> "Rate limit exceeded. Please retry after a moment.";
            case SERVICE_UNAVAILABLE -> "Service temporarily unavailable. Please retry shortly.";
            case GATEWAY_TIMEOUT   -> "Request timed out. Upstream service did not respond in time.";
            default                -> "An unexpected error occurred. Contact support if it persists.";
        };
    }

    private String buildJson(HttpStatus status, String message, String path, String traceId) {
        return String.format("""
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s","traceId":"%s"}""",
                Instant.now(), status.value(), status.getReasonPhrase(), message, path, traceId);
    }
}
