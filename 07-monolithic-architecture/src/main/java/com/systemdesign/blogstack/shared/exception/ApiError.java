package com.systemdesign.blogstack.shared.exception;

/**
 * Error response shape matching Nest's default {@code HttpException} JSON body
 * ({@code { statusCode, message, error } }), so client behavior porting from the NestJS API
 * doesn't have to change. {@code message} is a single string for most errors, or a list of
 * strings for validation failures (mirrors Nest's {@code ValidationPipe} array-of-messages body).
 */
public record ApiError(int statusCode, Object message, String error) {
}
