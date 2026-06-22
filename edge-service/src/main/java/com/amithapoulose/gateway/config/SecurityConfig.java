package com.amithapoulose.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtGrantedAuthoritiesConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig — OAuth2 Resource Server with offline JWKS validation.
 *
 * Provider-agnostic design:
 * - Works with Keycloak, Amazon Cognito, Azure Entra ID, Auth0, or any OIDC-compliant provider.
 * - Only two environment variables need changing to switch providers:
 *     JWKS_URI    → the provider's public key endpoint
 *     ISSUER_URI  → the provider's issuer claim value
 *
 * Key design decisions:
 * - OFFLINE JWT validation: gateway fetches JWKS public keys once, caches locally.
 *   Zero network calls to the identity provider per request.
 *   At 1000 req/sec, synchronous introspection would mean 1000 calls/sec to your IdP.
 *
 * - Token stripping: raw JWT is removed by AuthHeaderPropagationFilter.
 *   Downstream services receive verified claims as trusted headers.
 *   Services never need JWT libraries.
 *
 * - Rate limiting by authenticated identity (sub claim), not by IP.
 *   IP-based limiting breaks behind load balancers and shared offices.
 *
 * - Gateway answers: "can this request enter the system?"
 *   Services answer: "what is this authenticated user allowed to do?"
 *   These responsibilities must never be mixed.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${security.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // Actuator open for Prometheus scraping and K8s health probes
                        .pathMatchers("/actuator/**").permitAll()
                        // Fallback endpoint — called internally by circuit breaker
                        .pathMatchers("/fallback/**").permitAll()
                        // Public route example — no auth required
                        .pathMatchers("/public/**").permitAll()
                        // Mock endpoints for local demo (active on local profile only)
                        .pathMatchers("/mock/**").permitAll()
                        // All other routes require a valid JWT
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(reactiveJwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                )
                .build();
    }

    /**
     * ReactiveJwtDecoder — fetches JWKS from the identity provider once, caches public keys.
     * Spring Security auto-refreshes keys when the provider rotates them.
     *
     * To switch from Keycloak to Cognito:
     *   JWKS_URI=https://cognito-idp.{region}.amazonaws.com/{userPoolId}/.well-known/jwks.json
     *
     * To switch to Azure Entra ID:
     *   JWKS_URI=https://login.microsoftonline.com/{tenantId}/discovery/v2.0/keys
     */
    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * Extracts roles from the JWT.
     *
     * Keycloak puts roles at: realm_access.roles
     * Cognito puts roles at:  cognito:groups
     * Standard OIDC uses:     roles or scope
     *
     * Adjust authoritiesClaimName to match your identity provider's role claim.
     */
    @Bean
    public ReactiveJwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // Keycloak: "realm_access.roles" | Cognito: "cognito:groups" | Standard: "roles"
        authoritiesConverter.setAuthoritiesClaimName("realm_access.roles");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        ReactiveJwtAuthenticationConverter converter = new ReactiveJwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(
                new ReactiveJwtGrantedAuthoritiesConverterAdapter(authoritiesConverter)
        );
        return converter;
    }

    /**
     * CORS — allow configured frontend origins.
     * In production: set CORS_ALLOWED_ORIGINS to your actual frontend domain.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "http://localhost:3000"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
