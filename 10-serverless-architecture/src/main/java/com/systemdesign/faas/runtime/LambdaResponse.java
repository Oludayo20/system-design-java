package com.systemdesign.faas.runtime;

/**
 * Mirrors API Gateway's Lambda proxy-integration response shape ({@code statusCode} + body),
 * simplified to hold the body as a plain object instead of a JSON-encoded string — Spring's
 * Jackson message converter serializes it for us at the HTTP boundary, so double-encoding it
 * ourselves here would be redundant.
 */
public record LambdaResponse(int statusCode, Object body) {
}
