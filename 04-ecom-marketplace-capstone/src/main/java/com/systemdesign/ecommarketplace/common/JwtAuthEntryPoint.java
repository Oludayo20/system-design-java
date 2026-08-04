package com.systemdesign.ecommarketplace.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Reproduces JwtAuthGuard's two UnauthorizedException messages for requests
 * that reach a protected route without a valid principal: "Missing bearer
 * token" when no Authorization header was presented at all, "Invalid or
 * expired token" when one was presented but failed verification (see the
 * flag JwtAuthFilter sets on the request).
 */
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  public JwtAuthEntryPoint(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public void commence(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
      throws IOException, ServletException {
    boolean tokenWasInvalid = Boolean.TRUE.equals(request.getAttribute(JwtAuthFilter.TOKEN_INVALID_ATTRIBUTE));
    String message = tokenWasInvalid ? "Invalid or expired token" : "Missing bearer token";

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("statusCode", HttpStatus.UNAUTHORIZED.value());
    body.put("message", message);
    body.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
    body.put("timestamp", Instant.now().toString());

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.getWriter().write(objectMapper.writeValueAsString(body));
  }
}
