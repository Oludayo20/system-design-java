package com.systemdesign.blogstack.shared.exception;

/**
 * Mirrors Nest's {@code UnauthorizedException} -> HTTP 401 (used for business-logic auth
 * failures such as bad login credentials, as opposed to a missing/invalid JWT on a protected
 * route, which Spring Security's entry point handles separately).
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}
