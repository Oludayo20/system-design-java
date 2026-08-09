package com.systemdesign.faas.triggers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;

import com.systemdesign.faas.runtime.InvokeResult;

/**
 * Shared response mapping used by every HTTP-facing trigger adapter: surfaces cold/warm and
 * billing info in BOTH headers and the JSON body, so it is provable with a plain {@code curl -i}.
 */
final class TriggerResponses {

    private TriggerResponses() {
    }

    static ResponseEntity<Map<String, Object>> toResponseEntity(InvokeResult result) {
        Map<String, Object> body = new LinkedHashMap<>();
        Object responseBody = result.response().body();
        if (responseBody instanceof Map<?, ?> map) {
            map.forEach((key, value) -> body.put(String.valueOf(key), value));
        } else if (responseBody != null) {
            body.put("data", responseBody);
        }

        body.put("runtime", Map.of(
                "functionName", result.functionName(),
                "requestId", result.requestId(),
                "cold", result.cold(),
                "durationMs", result.durationMs(),
                "billedMs", result.billedMs(),
                "instanceId", result.instanceId()));

        return ResponseEntity.status(result.response().statusCode())
                .header("X-Cold-Start", String.valueOf(result.cold()))
                .header("X-Billed-Duration-Ms", String.valueOf(result.billedMs()))
                .header("X-Function-Name", result.functionName())
                .header("X-Request-Id", result.requestId())
                .body(body);
    }
}
