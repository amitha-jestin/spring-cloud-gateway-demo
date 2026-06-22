package com.amithapoulose.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * AuthHeaderPropagationFilter — Extract JWT claims and propagate as trusted headers.
 *
 * Pipeline position: SECOND — runs after TraceIdFilter, before rate limiter.
 * Why second: rate limiter needs the user identity header to key on.
 *
 * Gateway contract with downstream services:
 * - Downstream services NEVER see the raw JWT.
 * - They receive pre-validated, gateway-verified headers:
 *
 *   X-User-Id      → JWT sub claim (identity provider user UUID)
 *   X-User-Email   → email claim
 *   X-User-Roles   → comma-separated roles extracted from provider-specific claim
 *   X-User-Dept    → department custom claim (example of custom claim propagation)
 *
 * Benefits of this pattern:
 * 1. Decoupling — downstream services need no JWT library
 * 2. Security   — gateway is the single cryptographic validation point
 * 3. Performance — no repeated token parsing in each service
 * 4. Simplicity  — services read a String header, not a JWT
 *
 * If removing this filter: downstream services must validate tokens themselves.
 * That violates the single-boundary principle and adds latency + library coupling.
 */
@Slf4j
@Component
public class AuthHeaderPropagationFilter implements GatewayFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .flatMap(securityContext -> {
                    var authentication = securityContext.getAuthentication();

                    if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                        Jwt jwt = jwtAuth.getToken();

                        String userId = jwt.getSubject();
                        String email  = jwt.getClaimAsString("email");
                        String dept   = jwt.getClaimAsString("department"); // custom claim example
                        String roles  = extractRoles(jwt);

                        ServerWebExchange mutated = exchange.mutate()
                                .request(r -> r
                                        .header("X-User-Id",    nullSafe(userId))
                                        .header("X-User-Email", nullSafe(email))
                                        .header("X-User-Roles", nullSafe(roles))
                                        .header("X-User-Dept",  nullSafe(dept))
                                        // Strip raw JWT — downstream services don't need it
                                        .headers(headers -> headers.remove("Authorization"))
                                )
                                .build();

                        log.debug("Auth propagated: userId={} roles={}", userId, roles);
                        return chain.filter(mutated);
                    }

                    // No auth context — security config permits this path (e.g. /public)
                    return chain.filter(exchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    /**
     * Extracts roles from JWT.
     * Handles Keycloak's nested structure: realm_access.roles
     * For Cognito: change to jwt.getClaimAsStringList("cognito:groups")
     * For standard OIDC: change to jwt.getClaimAsStringList("roles")
     */
    @SuppressWarnings("unchecked")
    private String extractRoles(Jwt jwt) {
        try {
            // Keycloak structure: { "realm_access": { "roles": ["USER", "ADMIN"] } }
            var realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess != null && realmAccess.containsKey("roles")) {
                var roles = (java.util.List<String>) realmAccess.get("roles");
                return roles.stream()
                        .filter(r -> !r.equals("offline_access")
                                  && !r.equals("uma_authorization")
                                  && !r.startsWith("default-roles-"))
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
            }
        } catch (Exception e) {
            log.warn("Could not extract roles from JWT: {}", e.getMessage());
        }
        return "";
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }
}
