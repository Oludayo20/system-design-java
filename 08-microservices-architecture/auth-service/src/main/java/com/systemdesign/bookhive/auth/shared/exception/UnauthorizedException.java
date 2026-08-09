package com.systemdesign.bookhive.auth.shared.exception;

/** Mirrors Nest's {@code UnauthorizedException} -> HTTP 401. */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
