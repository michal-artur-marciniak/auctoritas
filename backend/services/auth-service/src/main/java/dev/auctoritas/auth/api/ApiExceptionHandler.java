package dev.auctoritas.auth.api;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
    String errorCode = ex.getReason() == null ? "request_failed" : ex.getReason();
    return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", errorCode));
  }
}
