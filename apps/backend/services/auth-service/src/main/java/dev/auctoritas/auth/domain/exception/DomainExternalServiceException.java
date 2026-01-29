package dev.auctoritas.auth.domain.exception;

/**
 * Signals a failure when interacting with an external dependency.
 */
public class DomainExternalServiceException extends DomainException {
  public DomainExternalServiceException(String errorCode) {
    super(errorCode);
  }

  public DomainExternalServiceException(String errorCode, Throwable cause) {
    super(errorCode, cause);
  }
}
