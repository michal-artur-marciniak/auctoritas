package dev.auctoritas.auth.domain.exception;

/**
 * Signals a missing domain resource.
 */
public class DomainNotFoundException extends DomainException {
  public DomainNotFoundException(String errorCode) {
    super(errorCode);
  }

  public DomainNotFoundException(String errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
