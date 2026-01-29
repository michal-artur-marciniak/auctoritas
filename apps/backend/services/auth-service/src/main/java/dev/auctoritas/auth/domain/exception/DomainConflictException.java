package dev.auctoritas.auth.domain.exception;

/**
 * Signals a domain conflict such as a uniqueness violation.
 */
public class DomainConflictException extends DomainException {
  public DomainConflictException(String errorCode) {
    super(errorCode);
  }

  public DomainConflictException(String errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
