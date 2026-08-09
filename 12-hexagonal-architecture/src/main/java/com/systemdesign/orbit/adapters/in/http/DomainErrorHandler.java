package com.systemdesign.orbit.adapters.in.http;

import com.systemdesign.orbit.core.domain.DomainError;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * The core throws plain DomainError subclasses — it has never heard of HTTP status codes. This
 * handler is where the REST inbound adapter decides how to translate a domain error into a
 * response. The CLI inbound adapter (OrbitCliRunner) makes its own, different decision — it just
 * prints the error and exits non-zero. Same core error, two different presentations.
 *
 * <p>Keyed by each error's own {@code code} (a plain string the core assigns, see
 * core/domain/DomainError.java) — deliberately not by class/instanceof, so this handler needs no
 * knowledge of the concrete error classes beyond the shared DomainError base.
 */
@RestControllerAdvice
public class DomainErrorHandler {

    private static final Map<String, HttpStatus> STATUS_BY_CODE = Map.ofEntries(
            Map.entry("SUBSCRIPTION_NOT_FOUND", HttpStatus.NOT_FOUND),
            Map.entry("CUSTOMER_ALREADY_SUBSCRIBED", HttpStatus.CONFLICT),
            Map.entry("DOWNGRADE_NOT_ALLOWED_MID_CYCLE", HttpStatus.CONFLICT),
            Map.entry("SAME_PLAN", HttpStatus.CONFLICT),
            Map.entry("SUBSCRIPTION_NOT_ACTIVE", HttpStatus.CONFLICT),
            Map.entry("ALREADY_CANCELED", HttpStatus.CONFLICT),
            Map.entry("PAYMENT_FAILED", HttpStatus.PAYMENT_REQUIRED),
            Map.entry("UNKNOWN_PLAN", HttpStatus.BAD_REQUEST));

    @ExceptionHandler(DomainError.class)
    public ResponseEntity<Map<String, Object>> handle(DomainError exception) {
        HttpStatus status = STATUS_BY_CODE.getOrDefault(exception.getCode(), HttpStatus.BAD_REQUEST);
        return ResponseEntity.status(status).body(Map.of(
                "statusCode", status.value(),
                "error", exception.getCode(),
                "message", exception.getMessage()));
    }
}
