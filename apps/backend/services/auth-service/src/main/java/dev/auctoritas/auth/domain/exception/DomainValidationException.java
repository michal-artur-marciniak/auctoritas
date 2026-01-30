package dev.auctoritas.auth.domain.exception;

/**
 * Signals a domain validation failure.
 */
public class DomainValidationException extends DomainException {
  public DomainValidationException(String errorCode) {
    super(errorCode);
  }

  public DomainValidationException(String errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
