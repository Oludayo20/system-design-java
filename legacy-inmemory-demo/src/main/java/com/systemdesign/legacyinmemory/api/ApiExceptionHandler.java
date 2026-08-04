package com.systemdesign.legacyinmemory.api;

import com.systemdesign.legacyinmemory.common.AppException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Java port of the Express error-handling middleware at the bottom of {@code api/server.js}:
 *
 * <pre>
 * app.use((err, req, res, next) => {
 *   res.status(err.statusCode || 500).json({ error: err.message });
 * });
 * </pre>
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<Map<String, Object>> handleAppException(AppException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        return ResponseEntity.status(500).body(Map.of("error", ex.getMessage()));
    }
}
