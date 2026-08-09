package com.systemdesign.bookhive.auth.shared.exception;

/** Mirrors Nest's {@code ConflictException} -> HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
