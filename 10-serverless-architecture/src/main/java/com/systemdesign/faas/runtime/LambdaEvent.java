package com.systemdesign.faas.runtime;

import java.util.Map;

/**
 * Mirrors the JSON-shaped event object AWS Lambda passes to a handler — here, whatever the
 * trigger adapter (HTTP body, simulated S3 key, schedule timestamp, queue message) put into it.
 */
public record LambdaEvent(Map<String, Object> payload) {

    public LambdaEvent {
        payload = payload == null ? Map.of() : Map.copyOf(payload);
    }

    public static LambdaEvent of(Map<String, Object> payload) {
        return new LambdaEvent(payload);
    }

    public Object get(String key) {
        return payload.get(key);
    }
}
