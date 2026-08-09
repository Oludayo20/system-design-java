package com.systemdesign.blogstack.shared.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * Central mapping from domain/validation exceptions to HTTP responses, playing the role Nest's
 * built-in exception filter + {@code ValidationPipe} play in the source project: every thrown
 * exception becomes a JSON body of the shape {@code { statusCode, message, error } }.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiError> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** jakarta.validation failures on @Valid request bodies (mirrors Nest's ValidationPipe). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<String> messages = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return build(HttpStatus.BAD_REQUEST, messages.isEmpty() ? List.of("Validation failed") : messages);
    }

    /** e.g. a path variable that fails {@code UUID.fromString(...)} -- mirrors ParseUUIDPipe. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return build(HttpStatus.BAD_REQUEST,
                "Validation failed (%s is expected to be a valid value for %s)"
                        .formatted(ex.getValue(), ex.getName()));
    }

    /** Malformed JSON, or an unrecognized field when strict deserialization is on. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return build(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, Object message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message, status.getReasonPhrase()));
    }
}
