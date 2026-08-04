package com.systemdesign.modularmonolith.identity.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.modularmonolith.shared.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Returns a JSON 401 body (instead of Spring Security's default HTML/empty response) for
 * unauthenticated requests to protected routes, matching Nest's default
 * {@code { statusCode, message, error } } shape for a rejected {@code JwtAuthGuard}.
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                          AuthenticationException authException) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError body = new ApiError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized", "Unauthorized");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
