package com.systemdesign.modularmonolith.shared.exception;

/** Mirrors Nest's {@code ForbiddenException} -> HTTP 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
