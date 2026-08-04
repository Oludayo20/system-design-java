package com.systemdesign.modularmonolith.shared.exception;

/** Mirrors Nest's {@code BadRequestException} -> HTTP 400. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
