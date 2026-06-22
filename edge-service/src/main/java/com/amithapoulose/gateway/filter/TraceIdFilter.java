package com.amithapoulose.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * TraceIdFilter — Inject or propagate a unique trace ID on every request.
 *
 * Pipeline position: FIRST — runs before all other filters.
 * Why first: all downstream filters need the trace ID for their own logs.
 *
 * Behaviour:
 * - If incoming request already has X-Trace-Id (e.g. from a client that
 *   generates its own correlation IDs), it is preserved.
 * - Otherwise a new UUID is generated.
 * - Trace ID is propagated to upstream services via request header.
 * - Trace ID is returned to clients via response header so they can correlate.
 *
 * This trace ID links logs across all services for a single request.
 * In Grafana Loki: search {app="api-gateway"} |= "your-trace-id" to find
 * every log line from every service for one request.
 */
@Slf4j
@Component
public class TraceIdFilter implements GatewayFilter {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        final String finalTraceId = traceId;

        // Propagate trace ID to upstream service
        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r
                        .header(TRACE_ID_HEADER, finalTraceId)
                        .header(REQUEST_ID_HEADER, finalTraceId)
                )
                .build();

        // Return trace ID to client in response
        mutatedExchange.getResponse().getHeaders().add(TRACE_ID_HEADER, finalTraceId);

        // Store in exchange attributes so other filters can read it
        mutatedExchange.getAttributes().put(TRACE_ID_HEADER, finalTraceId);

        log.debug("Trace ID: {}", finalTraceId);
        return chain.filter(mutatedExchange);
    }
}
