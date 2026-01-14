package dev.auctoritas.common.exception;

import dev.auctoritas.common.dto.ApiResponse;
import dev.auctoritas.common.dto.ErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(AuthException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e, HttpServletRequest request) {
    log.debug("Authentication error: {} - Code: {}", e.getMessage(), e.getErrorCode());

    String field = e.getDetails();
    ApiResponse<Void> response = ApiResponse.error(e.getMessage(), e.getErrorCode(), field);
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleSpringAuthException(AuthenticationException e, HttpServletRequest request) {
    log.debug("Spring authentication error: {}", e.getMessage());

    ApiResponse<Void> response = ApiResponse.error("Authentication failed", "AUTHENTICATION_FAILED");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(BadCredentialsException e, HttpServletRequest request) {
    log.debug("Bad credentials: {}", e.getMessage());

    ApiResponse<Void> response = ApiResponse.error("Invalid credentials", "INVALID_CREDENTIALS");
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
  }

  @ExceptionHandler(ValidationException.class)
  public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleValidationException(ValidationException e) {
    log.debug("Validation error: {} errors", e.getErrors().size());

    List<ErrorDetail> errorDetails = e.getErrors();
    if (errorDetails.isEmpty()) {
      errorDetails = List.of(new ErrorDetail("VALIDATION_ERROR", e.getMessage(), null));
    }

    ApiResponse<List<ErrorDetail>> response = new ApiResponse<>(false, e.getMessage(), errorDetails, null);
    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<List<ErrorDetail>>> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e) {
    log.debug("Method argument validation error: {} errors", e.getBindingResult().getErrorCount());

    List<ErrorDetail> errorDetails = e.getBindingResult().getFieldErrors().stream()
        .map(this::mapFieldError)
        .collect(Collectors.toList());

    ApiResponse<List<ErrorDetail>> response = new ApiResponse<>(false, "Validation failed", errorDetails, null);
    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(ServiceException.class)
  public ResponseEntity<ApiResponse<ErrorDetail>> handleServiceException(ServiceException e, HttpServletRequest request) {
    log.debug("Service error: {} - Code: {}", e.getMessage(), e.getErrorCode());

    ErrorDetail errorDetail = new ErrorDetail(
        e.getErrorCode(),
        e.getMessage(),
        e.getResourceType()
    );

    ApiResponse<ErrorDetail> response = new ApiResponse<>(false, e.getMessage(), errorDetail, null);

    HttpStatus status = determineHttpStatus(e.getErrorCode());
    return ResponseEntity.status(status).body(response);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<ErrorDetail>> handleIllegalArgumentException(IllegalArgumentException e) {
    log.debug("Illegal argument: {}", e.getMessage());

    ErrorDetail errorDetail = new ErrorDetail("INVALID_ARGUMENT", e.getMessage(), null);
    ApiResponse<ErrorDetail> response = new ApiResponse<>(false, e.getMessage(), errorDetail, null);
    return ResponseEntity.badRequest().body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<ErrorDetail>> handleGenericException(Exception e, HttpServletRequest request) {
    log.error("Unexpected error: {}", e.getMessage(), e);

    ErrorDetail errorDetail = new ErrorDetail("INTERNAL_ERROR", "An unexpected error occurred", null);
    ApiResponse<ErrorDetail> response = new ApiResponse<>(false, "An unexpected error occurred", errorDetail, null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  @ExceptionHandler(Throwable.class)
  public ResponseEntity<ApiResponse<ErrorDetail>> handleThrowable(Throwable t, HttpServletRequest request) {
    log.error("Critical error: {}", t.getMessage(), t);

    ErrorDetail errorDetail = new ErrorDetail("CRITICAL_ERROR", "A critical error occurred", null);
    ApiResponse<ErrorDetail> response = new ApiResponse<>(false, "A critical error occurred", errorDetail, null);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }

  private ErrorDetail mapFieldError(FieldError fieldError) {
    String code = fieldError.getCode() != null ? fieldError.getCode() : "INVALID";
    String message = fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "Invalid value";

    return new ErrorDetail(
        code.toUpperCase(),
        message,
        fieldError.getField()
    );
  }

  private HttpStatus determineHttpStatus(String errorCode) {
    if (errorCode == null) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    return switch (errorCode.toUpperCase()) {
      case "NOT_FOUND", "RESOURCE_NOT_FOUND" -> HttpStatus.NOT_FOUND;
      case "CONFLICT", "DUPLICATE" -> HttpStatus.CONFLICT;
      case "FORBIDDEN", "ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
      case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
      case "BAD_REQUEST", "INVALID_ARGUMENT" -> HttpStatus.BAD_REQUEST;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
