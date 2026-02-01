package dev.auctoritas.auth.interface.api;

import dev.auctoritas.auth.domain.exception.DomainConflictException;
import dev.auctoritas.auth.domain.exception.DomainException;
import dev.auctoritas.auth.domain.exception.DomainExternalServiceException;
import dev.auctoritas.auth.domain.exception.DomainForbiddenException;
import dev.auctoritas.auth.domain.exception.DomainNotFoundException;
import dev.auctoritas.auth.domain.exception.DomainUnauthorizedException;
import dev.auctoritas.auth.domain.exception.DomainValidationException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(DomainException.class)
  public ResponseEntity<Map<String, String>> handleDomainException(DomainException ex) {
    String errorCode = ex.getErrorCode() == null ? "request_failed" : ex.getErrorCode();
    return ResponseEntity.status(resolveStatus(ex)).body(Map.of("error", errorCode));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
    String errorCode = ex.getReason() == null ? "request_failed" : ex.getReason();
    return ResponseEntity.status(ex.getStatusCode()).body(Map.of("error", errorCode));
  }

  private HttpStatus resolveStatus(DomainException ex) {
    if (ex instanceof DomainValidationException) {
      return HttpStatus.BAD_REQUEST;
    }
    if (ex instanceof DomainConflictException) {
      return HttpStatus.CONFLICT;
    }
    if (ex instanceof DomainNotFoundException) {
      return HttpStatus.NOT_FOUND;
    }
    if (ex instanceof DomainUnauthorizedException) {
      return HttpStatus.UNAUTHORIZED;
    }
    if (ex instanceof DomainForbiddenException) {
      return HttpStatus.FORBIDDEN;
    }
    if (ex instanceof DomainExternalServiceException) {
      return HttpStatus.BAD_GATEWAY;
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
