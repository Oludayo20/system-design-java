package com.systemdesign.bookhive.gateway.proxy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * Routing table: gateway prefix -&gt; owning service. No path rewrite is performed - every
 * downstream controller is mounted at the same prefix the gateway routes on (e.g.
 * auth-service's AuthController is mounted at {@code /auth}, matching {@code /auth/**} here) -
 * so the request path passes through unchanged end to end.
 *
 * <p>The Authorization header is NOT inspected or stripped here - it passes straight through to
 * whichever service owns the route, and that service verifies the JWT itself with the shared
 * JWT_SECRET. That is the "auth check pass-through": the gateway's job is to make sure the
 * header survives the hop, not to be the one place auth is enforced.
 */
@RestController
public class ProxyController {

    private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

    private static final List<String> HOP_BY_HOP_REQUEST_HEADERS = List.of("host", "content-length", "connection");
    private static final List<String> HOP_BY_HOP_RESPONSE_HEADERS = List.of("transfer-encoding", "connection");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final List<Route> routes;

    public ProxyController(RestTemplate restTemplate,
                            ObjectMapper objectMapper,
                            @Value("${routes.auth-service-url}") String authServiceUrl,
                            @Value("${routes.catalog-service-url}") String catalogServiceUrl,
                            @Value("${routes.order-service-url}") String orderServiceUrl,
                            @Value("${routes.notification-service-url}") String notificationServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.routes = List.of(
                new Route("/auth", authServiceUrl),
                new Route("/books", catalogServiceUrl),
                new Route("/orders", orderServiceUrl),
                new Route("/notifications", notificationServiceUrl));
    }

    @RequestMapping("/**")
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI();

        Route route = routes.stream()
                .filter(r -> path.equals(r.prefix()) || path.startsWith(r.prefix() + "/"))
                .findFirst()
                .orElse(null);

        if (route == null) {
            return jsonError(HttpStatus.NOT_FOUND, "No route matches this path");
        }

        String query = request.getQueryString();
        String targetUrl = route.targetBaseUrl() + path + (query != null ? "?" + query : "");

        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            if (HOP_BY_HOP_REQUEST_HEADERS.contains(name.toLowerCase())) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(name);
            while (values.hasMoreElements()) {
                headers.add(name, values.nextElement());
            }
        }

        byte[] body = request.getInputStream().readAllBytes();
        HttpEntity<byte[]> entity = new HttpEntity<>(body.length == 0 ? null : body, headers);
        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(URI.create(targetUrl), method, entity, byte[].class);

            HttpHeaders responseHeaders = new HttpHeaders();
            response.getHeaders().forEach((name, values) -> {
                if (!HOP_BY_HOP_RESPONSE_HEADERS.contains(name.toLowerCase())) {
                    responseHeaders.addAll(name, values);
                }
            });

            return new ResponseEntity<>(response.getBody(), responseHeaders, response.getStatusCode());
        } catch (ResourceAccessException e) {
            log.error("proxy error for {} -> {}: {}", route.prefix(), route.targetBaseUrl(), e.getMessage());
            return jsonError(HttpStatus.BAD_GATEWAY, route.prefix() + " is temporarily unavailable");
        }
    }

    private ResponseEntity<byte[]> jsonError(HttpStatus status, String message) throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(Map.of("statusCode", status.value(), "message", message));
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(body);
    }
}
