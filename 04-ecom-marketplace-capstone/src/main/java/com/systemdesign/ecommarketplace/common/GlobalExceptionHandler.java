package com.systemdesign.ecommarketplace.common;

import com.systemdesign.ecommarketplace.common.exceptions.BadRequestException;
import com.systemdesign.ecommarketplace.common.exceptions.ConflictException;
import com.systemdesign.ecommarketplace.common.exceptions.NotFoundException;
import com.systemdesign.ecommarketplace.common.exceptions.UnauthorizedException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the small set of exceptions this app throws onto the same JSON error
 * shape Nest's default exception filter produces
 * ({ statusCode, message, error, timestamp }), so client behavior observed
 * against the original NestJS app carries over unchanged.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<Object> handleConflict(ConflictException ex) {
    return body(HttpStatus.CONFLICT, ex.getMessage());
  }

  @ExceptionHandler(NotFoundException.class)
  public ResponseEntity<Object> handleNotFound(NotFoundException ex) {
    return body(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<Object> handleBadRequest(BadRequestException ex) {
    return body(HttpStatus.BAD_REQUEST, ex.getMessage());
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<Object> handleUnauthorized(UnauthorizedException ex) {
    return body(HttpStatus.UNAUTHORIZED, ex.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
    return body(HttpStatus.UNAUTHORIZED, "Missing bearer token");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Object> handleValidation(MethodArgumentNotValidException ex) {
    List<String> messages =
        ex.getBindingResult().getFieldErrors().stream().map(FieldError::getDefaultMessage).toList();
    return body(HttpStatus.BAD_REQUEST, messages.isEmpty() ? "Validation failed" : messages.toString());
  }

  private ResponseEntity<Object> body(HttpStatus status, String message) {
    Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("statusCode", status.value());
    payload.put("message", message);
    payload.put("error", status.getReasonPhrase());
    payload.put("timestamp", Instant.now().toString());
    return ResponseEntity.status(status).body(payload);
  }
}
