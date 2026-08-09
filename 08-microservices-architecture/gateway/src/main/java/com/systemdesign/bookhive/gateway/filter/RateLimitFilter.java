package com.systemdesign.bookhive.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A minimal per-IP token-bucket rate limiter. Each client IP gets a bucket holding
 * {@code capacity} tokens; every request costs one token; tokens refill continuously at
 * {@code refillPerSecond}. When the bucket is empty the gateway returns 429 instead of ever
 * reaching a backend service - this is the kind of cross-cutting concern (auth pass-through,
 * rate limiting, routing) that belongs at the edge exactly once, rather than duplicated inside
 * every one of the four services behind it.
 *
 * <p>In-memory and per-process, which is fine for a single gateway container. A production
 * gateway fronting multiple gateway replicas would move this to Redis (same tradeoff as the
 * circuit breaker note in {@code 05-resilience}).
 */
@Component
@Order(2)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final double capacity;
    private final double refillPerSecond;
    private final ObjectMapper objectMapper;

    public RateLimitFilter(@Value("${rate-limit.capacity}") double capacity,
                            @Value("${rate-limit.refill-per-sec}") double refillPerSecond,
                            ObjectMapper objectMapper) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
        long now = System.currentTimeMillis();

        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, now));

        synchronized (bucket) {
            double elapsedSeconds = (now - bucket.lastRefillMs) / 1000.0;
            bucket.tokens = Math.min(capacity, bucket.tokens + elapsedSeconds * refillPerSecond);
            bucket.lastRefillMs = now;

            if (bucket.tokens < 1) {
                response.setHeader("Retry-After", "1");
                response.setStatus(429);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(objectMapper.writeValueAsString(
                        Map.of("statusCode", 429, "message", "Too many requests - slow down.")));
                return;
            }

            bucket.tokens -= 1;
        }

        filterChain.doFilter(request, response);
    }

    private static final class Bucket {
        private double tokens;
        private long lastRefillMs;

        private Bucket(double tokens, long lastRefillMs) {
            this.tokens = tokens;
            this.lastRefillMs = lastRefillMs;
        }
    }
}
