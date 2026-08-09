package com.systemdesign.bookhive.order.shared.exception;

/** catalog-service is unreachable, timed out, or returned an unexpected error -> HTTP 502. */
public class BadGatewayException extends RuntimeException {
    public BadGatewayException(String message) {
        super(message);
    }
}
