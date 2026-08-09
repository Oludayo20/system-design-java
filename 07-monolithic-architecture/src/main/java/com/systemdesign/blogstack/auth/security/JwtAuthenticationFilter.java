package com.systemdesign.blogstack.auth.security;

import com.systemdesign.blogstack.auth.AuthenticatedUser;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Extracts and validates the bearer JWT on every request, populating the
 * {@link org.springframework.security.core.context.SecurityContext} when it's valid. Combines the
 * roles NestJS splits into {@code JwtStrategy} (validate the token, build the principal) and
 * {@code JwtAuthGuard} (what every protected controller depends on) into one filter; which routes
 * actually require authentication is decided by {@link SecurityConfig}, not here -- an invalid or
 * missing token simply leaves the request unauthenticated and lets the filter chain continue, so
 * public routes (auth, GET posts/comments) are unaffected.
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            try {
                AuthenticatedUser user = jwtService.parseToken(token);
                SecurityContextHolder.getContext()
                        .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
            } catch (JwtException | IllegalArgumentException e) {
                log.debug("Rejecting invalid bearer token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
