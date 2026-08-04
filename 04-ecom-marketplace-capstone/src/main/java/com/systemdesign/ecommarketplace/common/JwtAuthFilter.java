package com.systemdesign.ecommarketplace.common;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Mirrors src/common/guards/jwt-auth.guard.ts: a single stateless check
 * against JWT_SECRET, with no server-side session store, which is exactly
 * what lets api-1 and api-2 validate tokens independently.
 *
 * <p>Runs on every request but only ever *populates* the SecurityContext
 * when a valid bearer token is present; whether a route actually requires
 * authentication is decided by SecurityConfig's authorizeHttpRequests rules
 * (auth/marketplace/health/docs are permitAll, everything else requires the
 * authentication this filter establishes) - the same split as Nest's
 * per-controller @UseGuards(JwtAuthGuard) vs. unguarded controllers.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

  public static final String TOKEN_INVALID_ATTRIBUTE = "jwtAuthFilter.tokenInvalid";

  private final JwtService jwtService;

  public JwtAuthFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String token = extractToken(request);
    if (token != null) {
      try {
        JwtPayload payload = jwtService.verify(token);
        var authentication = new UsernamePasswordAuthenticationToken(payload, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);
      } catch (JwtException | IllegalArgumentException ignored) {
        // Leave the SecurityContext empty; unauthenticated requests to a
        // protected route are rejected downstream by authorizeHttpRequests.
        // Flag *why* so JwtAuthEntryPoint can reproduce the guard's two
        // distinct messages ("Invalid or expired token" vs "Missing bearer
        // token"), same as JwtAuthGuard's two throw sites.
        SecurityContextHolder.clearContext();
        request.setAttribute(TOKEN_INVALID_ATTRIBUTE, Boolean.TRUE);
      }
    }
    filterChain.doFilter(request, response);
  }

  private String extractToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null) {
      return null;
    }
    String[] parts = header.split(" ", 2);
    if (parts.length == 2 && "Bearer".equals(parts[0])) {
      return parts[1];
    }
    return null;
  }
}
