package dev.auctoritas.auth.domain.exception;

import java.util.Objects;

/**
 * Base type for domain exceptions with stable error codes.
 */
public abstract class DomainException extends RuntimeException {
  private final String errorCode;

  protected DomainException(String errorCode) {
    super(errorCode);
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
  }

  protected DomainException(String errorCode, Throwable cause) {
    super(errorCode, cause);
    this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
  }

  public String getErrorCode() {
    return errorCode;
  }
}
