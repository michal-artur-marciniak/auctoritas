package dev.auctoritas.auth.domain.exception;

/**
 * Signals an authorization failure.
 */
public class DomainForbiddenException extends DomainException {
  public DomainForbiddenException(String errorCode) {
    super(errorCode);
  }

  public DomainForbiddenException(String errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
