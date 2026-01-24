package dev.auctoritas.common.exception;

import dev.auctoritas.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for standardized error responses across all services.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  /**
   * Handles custom Auctoritas business exceptions.
   */
  @ExceptionHandler(AuctoritasException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuctoritasException(AuctoritasException ex) {
    log.warn("Business exception: {} - {}", ex.getErrorCode(), ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error(ex.getErrorCode(), ex.getMessage()));
  }

  /**
   * Handles validation errors from @Valid annotations.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleValidationException(
      MethodArgumentNotValidException ex) {
    Map<String, Object> fieldErrors = new HashMap<>();
    ex.getBindingResult()
        .getFieldErrors()
        .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

    log.warn("Validation error: {}", fieldErrors);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("validation_error", "Validation failed", fieldErrors));
  }

  /**
   * Handles constraint violation exceptions.
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
      ConstraintViolationException ex) {
    Map<String, Object> violations = new HashMap<>();
    ex.getConstraintViolations()
        .forEach(
            violation ->
                violations.put(
                    violation.getPropertyPath().toString(), violation.getMessage()));

    log.warn("Constraint violation: {}", violations);
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("constraint_violation", "Constraint violation", violations));
  }

  /**
   * Handles illegal argument exceptions.
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
    log.warn("Illegal argument: {}", ex.getMessage());
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.error("invalid_argument", ex.getMessage()));
  }

  /**
   * Handles all other unhandled exceptions.
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiResponse.error("internal_error", "An unexpected error occurred"));
  }
}
