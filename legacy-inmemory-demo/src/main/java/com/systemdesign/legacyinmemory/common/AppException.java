package com.systemdesign.legacyinmemory.common;

import lombok.Getter;

/**
 * Mirrors the JS pattern used throughout the original modules of doing
 * {@code const err = new Error('...'); err.statusCode = 404; throw err;} and letting the
 * Express error-handling middleware in {@code api/server.js} respond with
 * {@code res.status(err.statusCode || 500).json({ error: err.message })}.
 *
 * @see com.systemdesign.legacyinmemory.api.ApiExceptionHandler
 */
@Getter
public class AppException extends RuntimeException {

    private final int statusCode;

    public AppException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
