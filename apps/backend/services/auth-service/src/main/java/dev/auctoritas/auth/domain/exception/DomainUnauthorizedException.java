package dev.auctoritas.auth.domain.exception;

/**
 * Signals an authentication failure.
 */
public class DomainUnauthorizedException extends DomainException {
  public DomainUnauthorizedException(String errorCode) {
    super(errorCode);
  }

  public DomainUnauthorizedException(String errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
