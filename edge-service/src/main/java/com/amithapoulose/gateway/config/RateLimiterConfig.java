package com.amithapoulose.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * RateLimiterConfig — Redis-backed token bucket rate limiter.
 *
 * Why Redis, not in-memory?
 * Multiple gateway instances each have their own in-memory counter.
 * A user can send N * limit requests if N gateway pods are running.
 * Redis provides a shared atomic counter across all instances.
 *
 * Token bucket algorithm:
 * - Tokens are added to the bucket at replenishRate per second.
 * - Each request consumes requestedTokens from the bucket.
 * - burstCapacity is the maximum bucket size — handles traffic spikes.
 *
 * Rate limiting by authenticated user identity (sub claim), not by IP.
 * IP-based limiting breaks behind load balancers and corporate NAT.
 * Identity-based limiting holds regardless of infrastructure topology.
 */
@Configuration
public class RateLimiterConfig {

    /**
     * Default rate limiter:
     * - 100 tokens/sec replenish rate (steady state)
     * - 200 max bucket size (burst capacity)
     * - 1 token per request
     *
     * Effectively: 100 req/sec steady, 200 req/sec burst.
     * Override per-route in application.yml if needed.
     */
    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(
                100,  // replenishRate — tokens added per second
                200,  // burstCapacity — max tokens in bucket
                1     // requestedTokens — consumed per request
        );
    }

    /**
     * Rate limit key resolver — uses authenticated user's subject claim.
     *
     * Falls back to "anonymous" for unauthenticated requests (e.g. /public routes).
     * Anonymous requests share one bucket — consider a lower limit for them.
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(principal -> principal.getName())
                .defaultIfEmpty("anonymous");
    }
}
