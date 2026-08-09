package com.systemdesign.bookhive.gateway.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Every request through the gateway gets one structured log line with the route it was matched
 * to and how long it took. This is the closest thing this demo has to distributed tracing - a
 * real system would attach a correlation/request ID header here and have every downstream
 * service echo it back into their own logs, so a single customer request can be followed across
 * all four services after the fact.
 */
@Component
@Order(1)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long start = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        response.setHeader("x-request-id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            String query = request.getQueryString();
            String uri = query != null ? request.getRequestURI() + "?" + query : request.getRequestURI();
            log.info("{} {} {} -> {} ({}ms)", requestId, request.getMethod(), uri, response.getStatus(), durationMs);
        }
    }
}
