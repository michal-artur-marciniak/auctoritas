package dev.auctoritas.common.exception;

public class AuthException extends RuntimeException {
  private final String errorCode;
  private final String details;

  public AuthException(String message) {
    super(message);
    this.errorCode = "AUTH_ERROR";
    this.details = null;
  }

  public AuthException(String message, String errorCode) {
    super(message);
    this.errorCode = errorCode;
    this.details = null;
  }

  public AuthException(String message, String errorCode, String details) {
    super(message);
    this.errorCode = errorCode;
    this.details = details;
  }

  public AuthException(String message, Throwable cause) {
    super(message, cause);
    this.errorCode = "AUTH_ERROR";
    this.details = null;
  }

  public AuthException(String message, String errorCode, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.details = null;
  }

  public AuthException(String message, String errorCode, String details, Throwable cause) {
    super(message, cause);
    this.errorCode = errorCode;
    this.details = details;
  }

  public String getErrorCode() {
    return errorCode;
  }

  public String getDetails() {
    return details;
  }
}
