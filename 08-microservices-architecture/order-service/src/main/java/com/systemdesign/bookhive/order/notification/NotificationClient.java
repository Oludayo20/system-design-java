package com.systemdesign.bookhive.order.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * Fire-and-forget call to notification-service, made over a real HTTP request (so the network
 * dependency is genuinely there, not simulated) - but bounded by a short timeout and wrapped so
 * ANY failure (timeout, connection refused, 5xx, DNS failure) is logged and swallowed rather
 * than propagated.
 *
 * <p>This is the fault-isolation lesson made real: order-service's job is to place orders, not
 * to guarantee a notification was sent. Stop notification-service entirely
 * ({@code docker compose stop notification-service}) and {@code placeOrder} still returns 201 -
 * see README "Fault isolation".
 *
 * <p>A production system would replace this synchronous best-effort call with a durable queue
 * so a failed notification could be retried instead of silently dropped - noted here, not
 * built, to keep this project focused on ONE lesson at a time.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final long timeoutMs;

    public NotificationClient(ObjectMapper objectMapper,
                               @Value("${notification.service-url}") String baseUrl,
                               @Value("${notification.timeout-ms}") long timeoutMs) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.timeoutMs = timeoutMs;
    }

    public void notifyOrderCreated(UUID orderId, UUID userId, UUID bookId, int quantity) {
        try {
            Map<String, Object> body = Map.of(
                    "type", "order.created",
                    "userId", userId.toString(),
                    "message", "Order " + orderId + " placed for " + quantity + "x book " + bookId,
                    "metadata", Map.of("orderId", orderId.toString(), "bookId", bookId.toString(), "quantity", quantity));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/notifications"))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("notification-service acknowledged order {}", orderId);
            } else {
                log.warn("notification-service returned {} for order {} - continuing anyway", response.statusCode(), orderId);
            }
        } catch (Exception error) {
            // Swallowed on purpose: a notification failure must never fail an order.
            log.warn("notification-service call failed for order {} ({}) - order still succeeds, "
                    + "this is fault isolation working as intended", orderId, error.getMessage());
        }
    }
}
