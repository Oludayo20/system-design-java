package com.systemdesign.bookhive.auth.shared.exception;

/**
 * Error response shape matching Nest's default {@code HttpException} JSON body
 * ({@code { statusCode, message, error } }), so client behavior porting from the NestJS API
 * doesn't have to change.
 */
public record ApiError(int statusCode, Object message, String error) {
}
