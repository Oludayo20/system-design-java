package com.systemdesign.modularmonolith.shared.exception;

/** Mirrors Nest's {@code NotFoundException} -> HTTP 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
