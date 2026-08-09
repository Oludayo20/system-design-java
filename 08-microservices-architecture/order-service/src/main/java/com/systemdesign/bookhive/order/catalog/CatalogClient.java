package com.systemdesign.bookhive.order.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.systemdesign.bookhive.order.shared.exception.BadGatewayException;
import com.systemdesign.bookhive.order.shared.exception.ConflictException;
import com.systemdesign.bookhive.order.shared.exception.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * order-service's ONLY way of learning a book's price or stock. There is no shortcut, no shared
 * table, no read replica of catalog-db - just this HTTP call. If catalog-service is
 * unreachable, placing an order fails loudly right here (unlike NotificationClient, this call
 * is NOT fire-and-forget: price/stock correctness matters for the order itself).
 *
 * <p>This is the "Good" half of the README's bad/good example:
 * <pre>
 *   Bad:  Orders Service directly queries the Catalog database.
 *   Good: Orders Service calls Catalog Service over HTTP.
 * </pre>
 */
@Component
public class CatalogClient {

    private static final Logger log = LoggerFactory.getLogger(CatalogClient.class);

    private final RestTemplate restTemplate;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public CatalogClient(RestTemplateBuilder builder,
                          @Value("${catalog.service-url}") String baseUrl,
                          ObjectMapper objectMapper) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    public ReservationResult reserveStock(UUID bookId, int quantity, String authorizationHeader) {
        String url = baseUrl + "/books/" + bookId + "/reserve";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authorizationHeader != null) {
            headers.set(HttpHeaders.AUTHORIZATION, authorizationHeader);
        }
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(Map.of("quantity", quantity), headers);

        try {
            return restTemplate.postForObject(url, entity, ReservationResult.class);
        } catch (HttpClientErrorException.NotFound e) {
            throw new NotFoundException("Book " + bookId + " not found in catalog");
        } catch (HttpClientErrorException.Conflict e) {
            throw new ConflictException(extractMessage(e, "Insufficient stock"));
        } catch (HttpStatusCodeException e) {
            log.error("catalog-service returned {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BadGatewayException("catalog-service rejected the reservation request");
        } catch (RestClientException e) {
            log.error("catalog-service unreachable at {}: {}", url, e.getMessage());
            throw new BadGatewayException("catalog-service is unreachable - could not place order");
        }
    }

    private String extractMessage(HttpStatusCodeException e, String fallback) {
        try {
            JsonNode node = objectMapper.readTree(e.getResponseBodyAsString());
            return node.hasNonNull("message") ? node.get("message").asText() : fallback;
        } catch (Exception parseError) {
            return fallback;
        }
    }
}
